import os
from qdrant_client import QdrantClient, models
from sentence_transformers import SentenceTransformer
from transformers import AutoTokenizer
from llama_cpp import Llama
import torch
import sys
import time

# Configuration
QDRANT_PATH = "./data/qdrant_db"
USE_MEMORY_CACHE = False # Toggle this to load the whole DB into RAM for faster retrieval
EMBEDDING_MODEL_PATH = "./model/Qwen3-Embedding-0.6B"
COLLECTION_NAME = "melopost_complete"
GGUF_MODEL_PATH = "model/Qwen3.5-0.8B-BF16.gguf"
TOKENIZER_PATH = "model/Qwen3.5-0.8B"
MODEL_PATH = "model/Qwen3.5-0.8B" # Use the transformers model path
MAX_CONTEXT_LENGTH = 32768 # Reduced from 152000 to prevent allocation errors
MAX_RETRIEVED_DOCS = 10 # Reduced from 50 for more focused context and less memory usage

def load_qdrant_to_memory(path, collection_name):
    """Loads a persistent Qdrant collection into a memory-only instance."""
    print(f"Loading Qdrant database from {path} into RAM cache...")
    disk_client = QdrantClient(path=path)
    
    if not disk_client.collection_exists(collection_name):
        return disk_client # Return original if collection doesn't exist
        
    info = disk_client.get_collection(collection_name)
    mem_client = QdrantClient(":memory:")
    
    # Replicate schema
    mem_client.create_collection(
        collection_name=collection_name,
        vectors_config=info.config.params.vectors,
        sparse_vectors_config=info.config.params.sparse_vectors
    )
    
    # Scroll all points
    scroll_result = disk_client.scroll(
        collection_name=collection_name,
        limit=info.points_count,
        with_payload=True,
        with_vectors=True
    )
    points = scroll_result[0]
    
    if points:
        mem_client.upsert(collection_name=collection_name, points=points)
    
    print(f"Memory cache ready ({len(points)} points).")
    return mem_client

def main():
    # 1. Initialize embedding model
    print(f"Loading embedding model from {EMBEDDING_MODEL_PATH}...")
    try:
        embedding_model = SentenceTransformer(EMBEDDING_MODEL_PATH, trust_remote_code=True)
    except Exception as e:
        print(f"Error loading embedding model: {e}")
        return

    # 2. Connect to Qdrant
    if not os.path.exists(QDRANT_PATH):
        print(f"Warning: Qdrant path '{QDRANT_PATH}' not found.")
    
    try:
        if USE_MEMORY_CACHE:
            qdrant_client = load_qdrant_to_memory(QDRANT_PATH, COLLECTION_NAME)
        else:
            print(f"Connecting to Qdrant at {QDRANT_PATH}...")
            qdrant_client = QdrantClient(path=QDRANT_PATH)

        if not qdrant_client.collection_exists(collection_name=COLLECTION_NAME):
            print(f"Error: Collection {COLLECTION_NAME} not found in Qdrant.")
            return
        
        stats = qdrant_client.get_collection(collection_name=COLLECTION_NAME)
        print(f"Connected to collection: {COLLECTION_NAME} (Count: {stats.points_count})")
    except Exception as e:
        print(f"Error connecting to Qdrant: {e}")
        print("Make sure you have run 'unified_rag_ingestion.py' first.")
        return

    # 3. Load the Qwen model
    print(f"Loading tokenizer from {TOKENIZER_PATH}...")
    if not os.path.exists(TOKENIZER_PATH):
        print(f"Error: Tokenizer directory '{TOKENIZER_PATH}' not found.")
        return
        
    try:
        # Load tokenizer
        tokenizer = AutoTokenizer.from_pretrained(TOKENIZER_PATH, trust_remote_code=True)
        
        print(f"Loading Qwen3.5 model from {MODEL_PATH}...")
        # Note: llama-cpp-python does not yet support the 'qwen35' architecture.
        # We use transformers with trust_remote_code=True as a reliable alternative.
        from transformers import AutoModelForMultimodalLM
        
        # Determine device
        device = "cpu"
        if torch.backends.mps.is_available():
            device = "mps"
        elif torch.cuda.is_available():
            device = "cuda"
            
        print(f"Using device: {device}")
        
        # Load with bfloat16 on MPS/CUDA if possible, else float32
        llm = AutoModelForMultimodalLM.from_pretrained(
            MODEL_PATH,
            torch_dtype=torch.bfloat16 if device in ["mps", "cuda"] else torch.float32,
            device_map=device,
            trust_remote_code=True
        )
    except Exception as e:
        print(f"Error loading Qwen model: {e}")
        return

    print("\n" + "="*50)
    print("RAG Chat Session Started. Type 'exit' or 'quit' to end.")
    print("="*50 + "\n")

    chat_history = []

    while True:
        try:
            query = input("User: ").strip()
            if query.lower() in ["exit", "quit"]:
                break
            if not query:
                continue

            # RAG: Retrieve context
            query_embedding = embedding_model.encode(query).tolist()
            
            try:
                # 1. Standard search for normal chunks
                search_results = qdrant_client.query_points(
                    collection_name=COLLECTION_NAME,
                    query=query_embedding,
                    limit=MAX_RETRIEVED_DOCS
                ).points

                # 2. Check if we need to explicitly pull summary records for "how many" or overview questions
                query_lower = query.lower()
                if any(word in query_lower for word in ["how many", "total", "count", "number of", "list all", "overview"]):
                    summary_results = qdrant_client.scroll(
                        collection_name=COLLECTION_NAME,
                        scroll_filter=models.Filter(
                            must=[
                                models.FieldCondition(
                                    key="source",
                                    match=models.MatchValue(value="summary"),
                                )
                            ]
                        ),
                        limit=1
                    )[0]
                    if summary_results:
                        # Prepend summary to results so it's prioritized in context
                        search_results = summary_results + search_results
            except Exception as e:
                print(f"Error searching Qdrant: {e}")
                search_results = []
            
            context = ""
            if search_results:
                # Use a set to avoid duplicate chunks if summary was also hit by vector search
                seen_ids = set()
                unique_results = []
                for hit in search_results:
                    if hit.id not in seen_ids:
                        unique_results.append(hit)
                        seen_ids.add(hit.id)
                context = "\n".join([f"- {hit.payload['text']}" for hit in unique_results])

            # Prepare System Prompt with Context
            system_prompt = (
                "You are a helpful assistant for the Melopost project. "
                "Use the following pieces of retrieved context to answer the user's question. "
                "If the context doesn't contain the answer, use your knowledge but mention that the context was insufficient.\n\n"
                "### Context:\n"
                f"{context}\n"
            )

            # Prepare messages for Qwen
            messages = [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": query}
            ]
            
            # Format using tokenizer's chat template
            text = tokenizer.apply_chat_template(
                messages,
                tokenize=False,
                add_generation_prompt=True
            )
            
            # Generate response
            inputs = tokenizer(
                text, 
                return_tensors="pt", 
                truncation=True, 
                max_length=MAX_CONTEXT_LENGTH
            ).to(llm.device)
            
            with torch.no_grad():
                output_ids = llm.generate(
                    **inputs,
                    max_new_tokens=512,
                    do_sample=True,
                    temperature=0.7,
                    top_p=0.9,
                    eos_token_id=tokenizer.eos_token_id,
                    pad_token_id=tokenizer.pad_token_id
                )
            
            # Decode only the new tokens
            response_text = tokenizer.decode(output_ids[0][inputs.input_ids.shape[1]:], skip_special_tokens=True)
            
            # Qwen3.5 reasoning models might output <think>...</think>. 
            # We can strip it if we want a clean answer, but usually it's better to keep it or handle it.
            # For this fix, we'll just present the full response.
            answer = response_text.strip()
            print(f"\nAssistant: {answer}\n")
            
        except KeyboardInterrupt:
            print("\nExiting...")
            break
        except Exception as e:
            print(f"An error occurred: {e}")

if __name__ == "__main__":
    main()

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForCausalLM
import uvicorn
from qdrant_client import QdrantClient, models
from sentence_transformers import SentenceTransformer
import os
import torch
import gc

# Initialize FastAPI app
app = FastAPI(title="LLM RAG API Server")

# Configuration for RAG
QDRANT_PATH = "./data/qdrant_db"
EMBEDDING_MODEL_PATH = "./model/Qwen3-Embedding-0.6B"
COLLECTION_NAME = "melopost_complete"

# Check if Qdrant path exists
if not os.path.exists(QDRANT_PATH):
    print(f"Warning: Qdrant path '{QDRANT_PATH}' not found. Retrieval will fail.")

# Initialize embedding model and Qdrant client
print(f"Initializing embedding model from {EMBEDDING_MODEL_PATH}...")
embedding_model = SentenceTransformer(EMBEDDING_MODEL_PATH, trust_remote_code=True)

print(f"Connecting to Qdrant at {QDRANT_PATH}...")
qdrant_client = QdrantClient(path=QDRANT_PATH)
try:
    if qdrant_client.collection_exists(collection_name=COLLECTION_NAME):
        collection_info = qdrant_client.get_collection(collection_name=COLLECTION_NAME)
        print(f"Connected to collection: {COLLECTION_NAME} (Count: {collection_info.points_count})")
    else:
        print(f"Error: Could not find collection '{COLLECTION_NAME}'")
except Exception as e:
    print(f"Error connecting to Qdrant: {e}")

# Load a more capable chat model (Qwen3.5-0.8B)
MODEL_PATH = "model/Qwen3.5-0.8B"
MAX_CONTEXT_LENGTH = 32768

print(f"Loading tokenizer from {MODEL_PATH}...")
tokenizer = AutoTokenizer.from_pretrained(MODEL_PATH, trust_remote_code=True)

print(f"Loading Qwen3.5 model from {MODEL_PATH}...")
# Determine device
device = "cpu"
if torch.backends.mps.is_available():
    device = "mps"
elif torch.cuda.is_available():
    device = "cuda"

print(f"Using device: {device}")

# Load model using transformers
from transformers import AutoModelForMultimodalLM
model = AutoModelForMultimodalLM.from_pretrained(
    MODEL_PATH,
    torch_dtype=torch.bfloat16 if device in ["mps", "cuda"] else torch.float32,
    device_map=device,
    trust_remote_code=True
)

class GenerationRequest(BaseModel):
    prompt: str
    max_new_tokens: int = 128
    temperature: float = 0.7
    do_sample: bool = True
    n_results: int = 3

class GenerationResponse(BaseModel):
    generated_text: str
    retrieved_context: list[str] = []

@app.post("/generate", response_model=GenerationResponse)
async def generate_text(request: GenerationRequest):
    try:
        retrieved_docs = []
        context_str = ""
        
        # Retrieval step
        if qdrant_client.collection_exists(collection_name=COLLECTION_NAME):
            query_embedding = embedding_model.encode(request.prompt).tolist()
            results = qdrant_client.query_points(
                collection_name=COLLECTION_NAME,
                query=query_embedding,
                limit=request.n_results
            ).points
            
            if results:
                retrieved_docs = [hit.payload['text'] for hit in results if hit.payload and 'text' in hit.payload]
                context_str = "\n\nRelevant Context:\n" + "\n---\n".join(retrieved_docs)
        
        # Wrap prompt in Qwen's chat template with retrieved context
        system_prompt = "You are a helpful assistant specialized in post-incident analysis (postmortems). Use the provided context to answer user queries when relevant."
        
        messages = [
            {"role": "system", "content": f"{system_prompt}{context_str}"},
            {"role": "user", "content": request.prompt}
        ]
        
        formatted_prompt = tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )

        # Generate response using transformers
        inputs = tokenizer(
            formatted_prompt,
            return_tensors="pt",
            truncation=True,
            max_length=MAX_CONTEXT_LENGTH
        ).to(model.device)

        with torch.no_grad():
            output_ids = model.generate(
                **inputs,
                max_new_tokens=request.max_new_tokens,
                do_sample=request.do_sample,
                temperature=request.temperature,
                top_p=0.9,
                eos_token_id=tokenizer.eos_token_id,
                pad_token_id=tokenizer.pad_token_id
            )

        # Decode only the new tokens
        assistant_reply = tokenizer.decode(output_ids[0][inputs.input_ids.shape[1]:], skip_special_tokens=True).strip()

        # Clean up to free memory
        del inputs
        del output_ids
        if device == "cuda":
            torch.cuda.empty_cache()
        elif device == "mps":
            torch.mps.empty_cache()
        gc.collect()

        return GenerationResponse(generated_text=assistant_reply, retrieved_context=retrieved_docs)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    rag_enabled = qdrant_client.collection_exists(collection_name=COLLECTION_NAME)
    count = 0
    if rag_enabled:
        count = qdrant_client.get_collection(collection_name=COLLECTION_NAME).points_count
        
    return {
        "status": "healthy",
        "model": EMBEDDING_MODEL_PATH,
        "rag_status": "enabled" if rag_enabled else "disabled",
        "collection_count": count
    }

if __name__ == "__main__":
    # Run the server on localhost:8000
    uvicorn.run(app, host="0.0.0.0", port=8000)

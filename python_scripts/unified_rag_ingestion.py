import io
import os
import sqlite3
import pandas as pd
import numpy as np
import hashlib
import pickle
import json
import glob
import subprocess
import time
import asyncio
import logging
import threading
from typing import List, Dict, Any, Optional
from collections import Counter
from concurrent.futures import ThreadPoolExecutor

import requests
import torch
import PyPDF2
from qdrant_client import QdrantClient, models
from sentence_transformers import SentenceTransformer
from langchain_text_splitters import RecursiveCharacterTextSplitter

# -------------------------
# CONFIGURATION
# -------------------------
API_BASE_URL = os.environ.get("API_BASE_URL", "http://localhost:8080/api")
API_USERNAME = os.environ.get("API_USERNAME", "admin")
API_PASSWORD = os.environ.get("API_PASSWORD", "admin")
QDRANT_PATH = "./data/qdrant_db"
COLLECTION_NAME = "melopost_complete"
EMBEDDING_MODEL_PATH = "./model/Qwen3-Embedding-0.6B"
EMBEDDING_CACHE_PATH = "./data/embedding_cache.db"
DOCS_PATH = "./docs"
POSTMORTEM_REPORTS_PATH = "./postmortem_reports"

DEFAULT_CHUNK_SIZE = 1536
MAX_CHUNK_SIZE = 4096
MIN_CHUNK_SIZE = 512
OVERLAP_PERCENTAGE = 0.25
BATCH_SIZE = 10
# -------------------------
# LOGGING
# -------------------------
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("output.log", mode="a")
    ]
)
logger = logging.getLogger(__name__)

# -------------------------
# DEVICE SELECTION
# -------------------------
if torch.backends.mps.is_available():
    DEVICE = "mps"
elif torch.cuda.is_available():
    DEVICE = "cuda"
else:
    DEVICE = "cpu"

os.environ["CUDA_VISIBLE_DEVICES"] = ""
os.environ["TOKENIZERS_PARALLELISM"] = "false"

# -------------------------
# EMBEDDING CACHE
# -------------------------
class EmbeddingCache:
    """A cache for embeddings to avoid redundant re-computation."""
    def __init__(self, cache_path):
        self.cache_path = cache_path
        os.makedirs(os.path.dirname(cache_path), exist_ok=True)
        self.conn = sqlite3.connect(cache_path, check_same_thread=False)
        self.create_table()

    def create_table(self):
        with self.conn:
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS metadata (
                    key TEXT PRIMARY KEY,
                    value TEXT
                )
            """)
            self.conn.execute("""
                CREATE TABLE IF NOT EXISTS embeddings (
                    hash TEXT PRIMARY KEY,
                    embedding BLOB
                )
            """)

    def set_model_path(self, model_path):
        cursor = self.conn.cursor()
        cursor.execute("SELECT value FROM metadata WHERE key = 'model_path'")
        row = cursor.fetchone()
        if row and row[0] != model_path:
            logger.info(f"Model path changed from '{row[0]}' to '{model_path}'. Clearing cache...")
            with self.conn:
                self.conn.execute("DELETE FROM embeddings")
                self.conn.execute("INSERT OR REPLACE INTO metadata (key, value) VALUES ('model_path', ?)", (model_path,))
        elif not row:
            with self.conn:
                self.conn.execute("INSERT INTO metadata (key, value) VALUES ('model_path', ?)", (model_path,))

    def get_hash(self, text):
        return hashlib.md5(text.encode("utf-8")).hexdigest()

    def get_embeddings(self, texts):
        hashes = [self.get_hash(t) for t in texts]
        placeholders = ",".join(["?"] * len(hashes))
        cursor = self.conn.cursor()
        cursor.execute(f"SELECT hash, embedding FROM embeddings WHERE hash IN ({placeholders})", tuple(hashes))
        cached_data = {row[0]: pickle.loads(row[1]) for row in cursor.fetchall()}
        
        results = []
        texts_to_embed = []
        indices_to_embed = []
        
        for i, h in enumerate(hashes):
            if h in cached_data:
                results.append(cached_data[h])
            else:
                results.append(None)
                texts_to_embed.append(texts[i])
                indices_to_embed.append(i)
        
        return results, texts_to_embed, indices_to_embed

    def save_embeddings(self, texts, embeddings):
        with self.conn:
            data = [
                (self.get_hash(t), pickle.dumps(e))
                for t, e in zip(texts, embeddings)
            ]
            self.conn.executemany("INSERT OR REPLACE INTO embeddings (hash, embedding) VALUES (?, ?)", data)

# -------------------------
# MARKDOWN UTILS
# -------------------------
def fetch_markdown_docs():
    """Reads all Markdown files from the docs directory."""
    logger.info(f"Reading Markdown files from {DOCS_PATH}...")
    markdown_data = []
    if not os.path.exists(DOCS_PATH):
        logger.warning(f"Docs path '{DOCS_PATH}' not found.")
        return markdown_data

    for md_file in glob.glob(os.path.join(DOCS_PATH, "*.md")):
        try:
            with open(md_file, "r", encoding="utf-8") as f:
                content = f.read()
                filename = os.path.basename(md_file)
                markdown_data.append({
                    "filename": filename,
                    "content": content,
                    "path": md_file
                })
                logger.info(f"Read {filename} ({len(content)} characters).")
        except Exception as e:
            logger.warning(f"Could not read {md_file}: {e}")
    return markdown_data

def fetch_pdf_docs_from_api():
    """Fetches PDF documents via the application API and extracts text in memory."""
    logger.info("Fetching PDF documents from Application API...")
    
    pdf_data = []
    auth = (API_USERNAME, API_PASSWORD)
    
    try:
        # 1. Get all postmortems
        response = requests.get(f"{API_BASE_URL}/postmortems", auth=auth)
        if response.status_code == 401:
            logger.error("Authentication failed for API access. Please check API_USERNAME and API_PASSWORD.")
            return pdf_data
        response.raise_for_status()
        
        # Verify content type is JSON
        content_type = response.headers.get('Content-Type', '')
        if 'application/json' not in content_type:
            logger.error(f"Expected JSON from API, but got '{content_type}'. The server might be redirecting to a login page.")
            return pdf_data
            
        postmortems = response.json()
        
        logger.info(f"Found {len(postmortems)} postmortems in the system.")
        
        for pm in postmortems:
            pm_id = pm.get('id')
            pm_title = pm.get('title', f"Postmortem {pm_id}")
            
            # 2. Get documents for each postmortem
            doc_response = requests.get(f"{API_BASE_URL}/postmortem-documents/postmortem/{pm_id}", auth=auth)
            doc_response.raise_for_status()
            
            # Verify content type is JSON
            doc_content_type = doc_response.headers.get('Content-Type', '')
            if 'application/json' not in doc_content_type:
                logger.warning(f"Expected JSON for documents of postmortem {pm_id}, but got '{doc_content_type}'. Skipping.")
                continue
                
            documents = doc_response.json()
            
            for doc in documents:
                doc_id = doc.get('id')
                filename = doc.get('fileName')
                
                # Only process PDF documents
                if filename and filename.lower().endswith('.pdf'):
                    logger.info(f"Downloading PDF: {filename} (ID: {doc_id}) for Postmortem: {pm_title}")
                    
                    # 3. Download the actual PDF data
                    download_response = requests.get(f"{API_BASE_URL}/postmortem-documents/{doc_id}/download", auth=auth)
                    download_response.raise_for_status()
                    bytes_data = download_response.content
                    
                    try:
                        with io.BytesIO(bytes_data) as f:
                            reader = PyPDF2.PdfReader(f)
                            content = ""
                            for page in reader.pages:
                                page_text = page.extract_text()
                                if page_text:
                                    content += page_text + "\n"
                            
                            if content.strip():
                                pdf_data.append({
                                    "filename": filename,
                                    "content": content,
                                    "path": f"api/postmortem-documents/{doc_id}",
                                    "postmortem_id": pm_id,
                                    "postmortem_title": pm_title
                                })
                                logger.info(f"Successfully extracted text from {filename} ({len(content)} characters).")
                            else:
                                logger.warning(f"No text content found in {filename}.")
                    except Exception as e:
                        logger.warning(f"Could not parse PDF {filename}: {e}")
                        
        return pdf_data
    except Exception as e:
        logger.error(f"Error fetching PDF documents from API: {e}")
        return pdf_data

# -------------------------
# SPARSE VECTOR HELPER
# -------------------------
def text_to_sparse_dict(text: str):
    """Converts text to a dictionary of {token_id: weight} for Qdrant sparse vector."""
    tokens = text.lower().split()
    counts = Counter(tokens)
    indices = []
    values = []
    for token, count in counts.items():
        token_id = int(hashlib.md5(token.encode()).hexdigest(), 16) % (2**31 - 1)
        indices.append(token_id)
        values.append(float(count))
    return models.SparseVector(indices=indices, values=values)

# -------------------------
# INGESTION ENGINE
# -------------------------
class UnifiedRAGIngestor:
    def __init__(self, recreate_collection=True):
        logger.info(f"Loading embedding model from {EMBEDDING_MODEL_PATH} to {DEVICE}...")
        self.embedder = SentenceTransformer(EMBEDDING_MODEL_PATH, device=DEVICE, trust_remote_code=True)
        self.dim = self.embedder.get_sentence_embedding_dimension()
        self.cache = EmbeddingCache(EMBEDDING_CACHE_PATH)
        self.cache.set_model_path(EMBEDDING_MODEL_PATH)
        
        # Initialize splitter with default settings; will be updated dynamically
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=DEFAULT_CHUNK_SIZE,
            chunk_overlap=int(DEFAULT_CHUNK_SIZE * OVERLAP_PERCENTAGE),
            separators=["\n\n", "\n", " ", ""]
        )
        
        os.makedirs(QDRANT_PATH, exist_ok=True)
        self.client = QdrantClient(path=QDRANT_PATH)
        self._setup_qdrant(recreate_collection)
        
        self.processed_chunks = 0
        self.total_chunks = 0
        self.table_relationships = {
            "POSTMORTEM": "Primary table for incident analysis.",
            "CHEESE_LAYER": "Linked to POSTMORTEM via POSTMORTEM_ID. Represents a defense layer in the Swiss Cheese Model.",
            "HOLE": "Linked to CHEESE_LAYER via LAYER_ID. Represents a weakness in a defense layer. May link to a STORY via STORY_ID.",
            "STORY": "Represents a remedial action task. Linked to HOLE via STORY_ID in the HOLE table.",
            "TIMELINE_EVENT": "Linked to POSTMORTEM via POSTMORTEM_ID. Chronological event of the incident.",
            "POSTMORTEM_QUESTION": "Linked to POSTMORTEM via POSTMORTEM_ID. Q&A for specific defense layers.",
            "USERS": "System user accounts.",
            "DATA_SOURCE": "Configuration for external systems like Jira or ServiceNow."
        }

    def _setup_qdrant(self, recreate_collection=True):
        if recreate_collection and self.client.collection_exists(COLLECTION_NAME):
            self.client.delete_collection(COLLECTION_NAME)
            logger.info(f"Dropped existing collection '{COLLECTION_NAME}'")

        if not self.client.collection_exists(COLLECTION_NAME):
            self.client.create_collection(
                collection_name=COLLECTION_NAME,
                vectors_config=models.VectorParams(
                    size=self.dim,
                    distance=models.Distance.COSINE
                ),
                sparse_vectors_config={
                    "sparse": models.SparseVectorParams(
                        index=models.SparseIndexParams(
                            on_disk=False,
                        )
                    )
                }
            )
            logger.info(f"Collection '{COLLECTION_NAME}' created in Qdrant with hybrid index.")
        else:
            logger.info(f"Using existing collection '{COLLECTION_NAME}' in Qdrant.")

    def close(self):
        if hasattr(self, "client") and self.client is not None:
            self.client.close()
            logger.info("Qdrant client closed.")

    async def log_progress(self):
        while True:
            await asyncio.sleep(5)
            if self.total_chunks > 0:
                percentage_done = (self.processed_chunks / self.total_chunks) * 100
                logger.info(f"Progress: {self.processed_chunks}/{self.total_chunks} chunks inserted ({percentage_done:.1f}% done)")

    async def embed_and_insert(self, batch: List[Dict[str, Any]]):
        texts = [x["text"] for x in batch]
        
        # 1. Check cache
        all_embeddings, texts_to_embed, indices_to_embed = self.cache.get_embeddings(texts)
        
        # 2. Compute missing embeddings
        if texts_to_embed:
            logger.info(f"Embedding {len(texts_to_embed)} new chunks...")
            loop = asyncio.get_running_loop()
            new_embeddings = await loop.run_in_executor(
                None,
                lambda: self.embedder.encode(
                    texts_to_embed,
                    normalize_embeddings=True,
                    show_progress_bar=False,
                    device=DEVICE
                ).tolist()
            )
            self.cache.save_embeddings(texts_to_embed, new_embeddings)
            for idx, embedding in zip(indices_to_embed, new_embeddings):
                all_embeddings[idx] = embedding

        # 3. Prepare Points
        points = []
        for i, item in enumerate(batch):
            text = item["text"]
            # Create a unique ID based on content and metadata
            meta_str = json.dumps(item.get("metadata", {}), sort_keys=True)
            content_id = hashlib.md5(f"{text}_{meta_str}".encode()).hexdigest()
            
            sparse_vec = text_to_sparse_dict(text)
            
            payload = {
                "text": text
            }
            if "metadata" in item:
                payload.update(item["metadata"])
            
            points.append(models.PointStruct(
                id=content_id,
                vector={
                    "": [float(v) for v in all_embeddings[i]],
                    "sparse": sparse_vec
                },
                payload=payload
            ))

        # 4. Insert with retry logic
        max_retries = 3
        for attempt in range(max_retries):
            try:
                self.client.upsert(
                    collection_name=COLLECTION_NAME,
                    points=points
                )
                break
            except Exception as e:
                logger.warning(f"Qdrant insertion error (attempt {attempt+1}): {e}")
                if attempt < max_retries - 1:
                    await asyncio.sleep(2)
                else:
                    logger.error(f"Failed to insert batch after {max_retries} attempts.")
                    raise e

        self.processed_chunks += len(points)
        # Short break between batches
        await asyncio.sleep(0.5)

    def get_dynamic_chunking_params(self, content: str):
        """Calculates dynamic chunk_size and chunk_overlap based on content length."""
        content_len = len(content)
        # Strategy: aim for about 10 chunks, but keep within [MIN_CHUNK_SIZE, MAX_CHUNK_SIZE]
        target_chunk_size = max(MIN_CHUNK_SIZE, min(MAX_CHUNK_SIZE, content_len // 10))
        chunk_overlap = int(target_chunk_size * OVERLAP_PERCENTAGE)
        return target_chunk_size, chunk_overlap

    def prepare_data(self, all_data: Dict[str, pd.DataFrame], markdown_docs: List[Dict[str, str]], pdf_docs: List[Dict[str, str]]):
        all_items = []
        
        # Process database tables - SKIPPED AS PER REQUIREMENT
        # for table_name, df in all_data.items():
        #     ... (existing code)
        
        # Process markdown docs
        for doc in markdown_docs:
            # Dynamic chunking for each document
            chunk_size, chunk_overlap = self.get_dynamic_chunking_params(doc["content"])
            self.text_splitter._chunk_size = chunk_size
            self.text_splitter._chunk_overlap = chunk_overlap
            logger.info(f"Chunking {doc['filename']} with size={chunk_size}, overlap={chunk_overlap}...")

            # Wrap markdown content in a JSON structure
            doc_dict = {
                "source": "markdown",
                "filename": doc["filename"],
                "path": doc["path"],
                "content": doc["content"]
            }
            # (no changes below here in this loop, just for context)
            doc_json = json.dumps(doc_dict, indent=2)
            
            # Since we want each record to be a JSON, we SHOULD NOT chunk it if we want to guarantee valid JSON
            # However, markdown files can be huge. 
            # If we MUST chunk, we can't easily maintain JSON validity for each chunk using RecursiveCharacterTextSplitter.
            # For now, I'll avoid chunking if the document is reasonably sized, or just accept that chunks might break JSON.
            # Actually, to fulfill "make every record that you upload to the RAG a JSON format", 
            # each chunk itself should be a valid JSON.
            
            # Better strategy: chunk the content FIRST, then wrap each chunk in JSON.
            content_chunks = self.text_splitter.split_text(doc["content"])
            for i, content_chunk in enumerate(content_chunks):
                chunk_dict = {
                    "source": "markdown",
                    "filename": doc["filename"],
                    "path": doc["path"],
                    "content": content_chunk,
                    "chunk_index": i,
                    "total_chunks": len(content_chunks)
                }
                chunk_json = json.dumps(chunk_dict, indent=2)
                all_items.append({
                    "text": chunk_json,
                    "metadata": {
                        "source": "markdown",
                        "filename": doc["filename"],
                        "path": doc["path"],
                        "chunk_index": i
                    }
                })
            
        # Process PDF docs from database
        for doc in pdf_docs:
            # Dynamic chunking for each document
            chunk_size, chunk_overlap = self.get_dynamic_chunking_params(doc["content"])
            self.text_splitter._chunk_size = chunk_size
            self.text_splitter._chunk_overlap = chunk_overlap
            logger.info(f"Chunking {doc['filename']} with size={chunk_size}, overlap={chunk_overlap}...")

            content_chunks = self.text_splitter.split_text(doc["content"])
            for i, content_chunk in enumerate(content_chunks):
                chunk_dict = {
                    "source": "postmortem_report",
                    "filename": doc["filename"],
                    "path": doc["path"],
                    "postmortem_id": doc.get("postmortem_id"),
                    "postmortem_title": doc.get("postmortem_title"),
                    "content": content_chunk,
                    "chunk_index": i,
                    "total_chunks": len(content_chunks)
                }
                chunk_json = json.dumps(chunk_dict, indent=2)
                all_items.append({
                    "text": chunk_json,
                    "metadata": {
                        "source": "postmortem_report",
                        "filename": doc["filename"],
                        "path": doc["path"],
                        "postmortem_id": doc.get("postmortem_id"),
                        "chunk_index": i
                    }
                })
        
        # Add a summary record for all postmortem reports to answer "how many" questions
        if pdf_docs:
            summary_dict = {
                "source": "summary",
                "type": "postmortem_reports_overview",
                "total_count": len(pdf_docs),
                "filenames": [doc["filename"] for doc in pdf_docs],
                "description": f"There are a total of {len(pdf_docs)} unique postmortem reports available in the Melopost system."
            }
            summary_json = json.dumps(summary_dict, indent=2)
            all_items.append({
                "text": summary_json,
                "metadata": {
                    "source": "summary",
                    "type": "postmortem_reports_overview"
                }
            })
        
        return all_items

    async def run(self):
        # 1. Fetch data
        markdown_docs = fetch_markdown_docs()
        pdf_docs = fetch_pdf_docs_from_api() # Updated: Fetch PDFs from Application API
        
        # 2. Prepare chunks
        logger.info("Preparing and chunking data...")
        all_items = self.prepare_data({}, markdown_docs, pdf_docs)
        self.total_chunks = len(all_items)
        logger.info(f"Total chunks to process: {self.total_chunks}")
        
        # 3. Start progress logger
        log_task = asyncio.create_task(self.log_progress())
        
        # 4. Ingest in batches
        semaphore = asyncio.Semaphore(1)
        async def wrapped_ingest(b):
            async with semaphore:
                await self.embed_and_insert(b)

        for i in range(0, self.total_chunks, BATCH_SIZE):
            batch = all_items[i : i + BATCH_SIZE]
            await wrapped_ingest(batch)
            
        log_task.cancel()
        logger.info("✅ Unified RAG Ingestion Complete.")

if __name__ == "__main__":
    ingestor = UnifiedRAGIngestor()
    try:
        asyncio.run(ingestor.run())
    finally:
        ingestor.close()

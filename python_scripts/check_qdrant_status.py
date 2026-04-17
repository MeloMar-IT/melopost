import os
from qdrant_client import QdrantClient

QDRANT_PATH = "./data/qdrant_db"
COLLECTION_NAME = "melopost_complete"

def check_qdrant():
    if not os.path.exists(QDRANT_PATH):
        print(f"Error: Qdrant DB path not found at {QDRANT_PATH}")
        return

    client = QdrantClient(path=QDRANT_PATH)
    
    collections = client.get_collections()
    print(f"Collections: {[c.name for c in collections.collections]}")
    
    if COLLECTION_NAME in [c.name for c in collections.collections]:
        info = client.get_collection(collection_name=COLLECTION_NAME)
        print(f"\nCollection: {COLLECTION_NAME}")
        print(f"Points count: {info.points_count}")
        
        # Sample query
        res, _ = client.scroll(
            collection_name=COLLECTION_NAME,
            limit=100,
            with_payload=True,
            with_vectors=False
        )
        print("\nSample records:")
        for r in res:
            payload = r.payload
            source = payload.get('source', 'unknown')
            filename = payload.get('filename', 'N/A')
            pm_id = payload.get('postmortem_id', 'N/A')
            print(f"- ID: {r.id}, Source: {source}, Filename: {filename}, PM ID: {pm_id}")
            text_preview = str(payload.get('text', ''))[:100].replace('\n', ' ') + "..."
            print(f"  Text: {text_preview}")
    else:
        print(f"\nCollection '{COLLECTION_NAME}' not found.")

if __name__ == "__main__":
    check_qdrant()

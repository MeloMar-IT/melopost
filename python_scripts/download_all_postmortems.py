import requests
import os
import sys
import io
import PyPDF2
import asyncio
import logging
from requests.auth import HTTPBasicAuth
from unified_rag_ingestion import UnifiedRAGIngestor

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

async def ingest_pdf_to_rag(ingestor, pdf_bytes, filename, pm_id, pm_title):
    """
    Extracts text from PDF bytes and ingests it into the RAG.
    """
    logger.info(f"Ingesting {filename} into RAG...")
    try:
        with io.BytesIO(pdf_bytes) as f:
            reader = PyPDF2.PdfReader(f)
            content = ""
            for page in reader.pages:
                page_text = page.extract_text()
                if page_text:
                    content += page_text + "\n"
            
            if not content.strip():
                logger.warning(f"No text content found in {filename}. Skipping RAG ingestion.")
                return False

            doc = {
                "filename": filename,
                "content": content,
                "path": f"memory://{filename}",
                "postmortem_id": pm_id,
                "postmortem_title": pm_title
            }
            
            # Use ingestor to prepare and insert data
            all_items = ingestor.prepare_data({}, [], [doc])
            if all_items:
                await ingestor.embed_and_insert(all_items)
                logger.info(f"✅ Successfully ingested {filename} into RAG ({len(all_items)} chunks).")
                return True
            else:
                logger.warning(f"Failed to prepare data for {filename}.")
                return False
    except Exception as e:
        logger.error(f"Error ingesting {filename} into RAG: {e}")
        return False

async def download_all_postmortem_pdfs(base_url="http://localhost:8080", username="admin", password="admin"):
    """
    Downloads all postmortem PDF reports from the Melopost API and ingests them into RAG (memory-only).
    """
    # Initialize RAG ingestor (do not recreate collection)
    ingestor = UnifiedRAGIngestor(recreate_collection=False)

    # API endpoints
    list_url = f"{base_url}/api/postmortems"
    auth = HTTPBasicAuth(username, password)
    
    logger.info(f"Fetching postmortem list from {list_url}...")
    try:
        response = requests.get(list_url, auth=auth, allow_redirects=False)
        if response.status_code == 302:
            logger.error(f"Error: Server redirected to {response.headers.get('Location')}. Authentication might have failed or the endpoint is misconfigured.")
            return
        response.raise_for_status()
        
        content_type = response.headers.get('Content-Type', '')
        if 'application/json' not in content_type:
            logger.error(f"Error: Expected JSON response, but got {content_type}. Authentication might have failed.")
            return
            
        postmortems = response.json()
    except requests.exceptions.RequestException as e:
        logger.error(f"Error fetching postmortem list: {e}")
        return
    except ValueError:
        logger.error("Error parsing JSON response from postmortem list.")
        return

    logger.info(f"Found {len(postmortems)} postmortems. Starting downloads and ingestion...")

    downloaded_count = 0
    ingested_count = 0
    for pm in postmortems:
        pm_id = pm.get('id')
        pm_title = pm.get('title', f'postmortem-{pm_id}').replace('/', '_').replace('\\', '_')
        
        if pm_id is None:
            logger.warning("Skipping entry with no ID.")
            continue

        report_url = f"{base_url}/api/postmortems/{pm_id}/report"
        filename = f"postmortem-report-{pm_id}-{pm_title}.pdf"

        logger.info(f"Downloading report for ID {pm_id} ({pm_title})...")
        try:
            report_response = requests.get(report_url, auth=auth, stream=True)
            if report_response.status_code == 200:
                pdf_bytes = report_response.content
                logger.info(f"  Downloaded {len(pdf_bytes)} bytes")
                downloaded_count += 1
                
                # Ingest into RAG
                success = await ingest_pdf_to_rag(ingestor, pdf_bytes, filename, pm_id, pm_title)
                if success:
                    ingested_count += 1
            else:
                logger.error(f"  Failed to download ID {pm_id}: HTTP {report_response.status_code}")
        except requests.exceptions.RequestException as e:
            logger.error(f"  Error downloading ID {pm_id}: {e}")

    logger.info(f"\nFinished. Downloaded {downloaded_count} and ingested {ingested_count} of {len(postmortems)} reports.")
    ingestor.close()

if __name__ == "__main__":
    # You can change the base URL if needed
    url = os.environ.get("MELOPOST_URL", "http://localhost:8080")
    user = os.environ.get("MELOPOST_USER", "admin")
    pw = os.environ.get("MELOPOST_PASSWORD", "admin")

    if len(sys.argv) > 1:
        url = sys.argv[1]
    if len(sys.argv) > 3:
        user = sys.argv[2]
        pw = sys.argv[3]
    
    asyncio.run(download_all_postmortem_pdfs(url, user, pw))

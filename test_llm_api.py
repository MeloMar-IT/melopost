import requests
import json
import sys
import time

def test_api():
    url = "http://localhost:8000/generate"
    payload = {
        "prompt": "What is the capital of France?",
        "max_new_tokens": 50
    }
    
    print("Sending request to LLM API...")
    try:
        response = requests.post(url, json=payload)
        if response.status_code == 200:
            print("\nResponse successful!")
            result = response.json()
            print(f"Generated Text: {result.get('generated_text')}")
        else:
            print(f"\nError: {response.status_code}")
            print(response.text)
    except requests.exceptions.ConnectionError:
        print("\nCould not connect to the API. Make sure llm_api.py is running on port 8000.")

def check_health():
    url = "http://localhost:8000/health"
    try:
        response = requests.get(url)
        if response.status_code == 200:
            print("Server is healthy!")
            return True
        return False
    except:
        return False

if __name__ == "__main__":
    if not check_health():
        print("Waiting for server to start...")
        # Note: In a real scenario, we'd wait or start the process. 
        # For this script, we assume the user starts the server in another terminal.
        sys.exit(1)
    test_api()

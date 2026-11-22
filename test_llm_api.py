import os
import sys

import requests

BASE = "http://127.0.0.1:%s" % os.environ.get("LLM_API_PORT", "8000")


def auth_headers():
    return {"Authorization": "Bearer " + os.environ["LLM_API_TOKEN"]}


def test_api():
    url = BASE + "/generate"
    payload = {
        "prompt": "What is the capital of France?",
        "max_new_tokens": 50
    }

    print("Sending request to LLM API...")
    try:
        response = requests.post(url, json=payload, headers=auth_headers(), timeout=120)
        if response.status_code == 200:
            print("\nResponse successful!")
            result = response.json()
            print(f"Generated Text: {result.get('generated_text')}")
        else:
            print(f"\nError: {response.status_code}")
            print(response.text)
    except requests.exceptions.ConnectionError:
        print("\nCould not connect to the API. Make sure llm_api.py is running.")


def check_health():
    url = BASE + "/health"
    try:
        response = requests.get(url, headers=auth_headers(), timeout=10)
        if response.status_code == 200:
            print("Server is healthy!")
            return True
        return False
    except requests.exceptions.RequestException:
        return False


if __name__ == "__main__":
    if not os.environ.get("LLM_API_TOKEN"):
        sys.exit("LLM_API_TOKEN must be set to the helper's token")
    if not check_health():
        print("Waiting for server to start...")
        # We assume the user starts the server in another terminal.
        sys.exit(1)
    test_api()

import json
import os
import socket
import sys
import threading
import time
import unittest
import urllib.error
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))

import uvicorn  # noqa: E402
from fastapi import HTTPException  # noqa: E402

import llm_api  # noqa: E402

TOKEN = "t" * 40


class FakeGenerator:
    class tokenizer:
        eos_token_id = 0

    def __call__(self, prompt, **kwargs):
        return [{"generated_text": prompt + "ok"}]


class FailingGenerator(FakeGenerator):
    def __call__(self, prompt, **kwargs):
        raise RuntimeError("secret-internal-detail")


class UnitTests(unittest.TestCase):
    def test_host_is_loopback(self):
        self.assertEqual(llm_api.HOST, "127.0.0.1")

    def test_load_token_missing_or_short_exits(self):
        for env in ({}, {"LLM_API_TOKEN": ""}, {"LLM_API_TOKEN": "   "}, {"LLM_API_TOKEN": "a" * 31}):
            with self.assertRaises(SystemExit) as cm:
                llm_api.load_token(env)
            self.assertNotIn("a" * 31, str(cm.exception))

    def test_load_token_valid(self):
        self.assertEqual(llm_api.load_token({"LLM_API_TOKEN": "a" * 32}), "a" * 32)

    def test_load_port(self):
        self.assertEqual(llm_api.load_port({}), 8000)
        self.assertEqual(llm_api.load_port({"LLM_API_PORT": "8123"}), 8123)
        for bad in ("abc", "0", "70000"):
            with self.assertRaises(SystemExit):
                llm_api.load_port({"LLM_API_PORT": bad})

    def test_token_check(self):
        check = llm_api.make_token_check(TOKEN)
        for bad in (None, "Bearer wrong", "Basic " + TOKEN, "Bearer", "bearer" + TOKEN):
            with self.assertRaises(HTTPException) as cm:
                check(bad)
            self.assertEqual(cm.exception.status_code, 401)
        self.assertIsNone(check("Bearer " + TOKEN))

    def test_docs_disabled(self):
        app = llm_api.create_app(TOKEN, FakeGenerator())
        self.assertIsNone(app.docs_url)
        self.assertIsNone(app.redoc_url)
        self.assertIsNone(app.openapi_url)


class ServerTests(unittest.TestCase):
    def start(self, generator):
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.bind(("127.0.0.1", 0))
        port = sock.getsockname()[1]
        app = llm_api.create_app(TOKEN, generator)
        server = uvicorn.Server(uvicorn.Config(app, log_level="warning"))
        thread = threading.Thread(target=server.run, kwargs={"sockets": [sock]}, daemon=True)

        def cleanup():
            server.should_exit = True
            thread.join(timeout=10)
            alive = thread.is_alive()
            sock.close()
            self.assertFalse(alive, "uvicorn thread did not stop")

        self.addCleanup(cleanup)
        thread.start()
        deadline = time.monotonic() + 10
        while not server.started:
            if time.monotonic() > deadline or not thread.is_alive():
                self.fail("uvicorn server did not start within 10s")
            time.sleep(0.02)
        self.base = "http://127.0.0.1:%d" % port

    def call(self, path, token=None, body=None, scheme="Bearer"):
        headers = {}
        if token is not None:
            headers["Authorization"] = "%s %s" % (scheme, token)
        data = None
        if body is not None:
            data = json.dumps(body).encode()
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(self.base + path, data=data, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=5) as r:
                return r.status, r.read().decode()
        except urllib.error.HTTPError as e:
            try:
                return e.code, e.read().decode()
            finally:
                e.close()

    def test_health_auth(self):
        self.start(FakeGenerator())
        self.assertEqual(self.call("/health")[0], 401)
        self.assertEqual(self.call("/health", "wrong")[0], 401)
        self.assertEqual(self.call("/health", TOKEN, scheme="Basic")[0], 401)
        status, body = self.call("/health", TOKEN)
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body)["status"], "healthy")

    def test_generate_auth_and_validation(self):
        self.start(FakeGenerator())
        self.assertEqual(self.call("/generate", body={"prompt": "hi"})[0], 401)
        status, body = self.call("/generate", TOKEN, {"prompt": "hi"})
        self.assertEqual(status, 200)
        self.assertEqual(json.loads(body)["generated_text"], "ok")
        self.assertEqual(self.call("/generate", TOKEN, {"prompt": "hi", "max_new_tokens": 0})[0], 422)

    def test_docs_not_served(self):
        self.start(FakeGenerator())
        self.assertEqual(self.call("/docs", TOKEN)[0], 404)
        self.assertEqual(self.call("/openapi.json", TOKEN)[0], 404)

    def test_generation_error_is_generic(self):
        self.start(FailingGenerator())
        status, body = self.call("/generate", TOKEN, {"prompt": "hi"})
        self.assertEqual(status, 500)
        self.assertNotIn("secret-internal-detail", body)


if __name__ == "__main__":
    unittest.main()

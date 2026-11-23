"""Local LLM helper. Binds to 127.0.0.1 only; every endpoint needs a Bearer token."""
import hmac
import logging
import os
from typing import Optional

import uvicorn
from fastapi import Depends, FastAPI, Header, HTTPException
from pydantic import BaseModel, Field

HOST = "127.0.0.1"
DEFAULT_PORT = 8000
MIN_TOKEN_LENGTH = 32
MODEL_ID = "TinyLlama/TinyLlama-1.1B-Chat-v1.0"

logger = logging.getLogger("llm_api")


def load_token(environ=os.environ) -> str:
    token = environ.get("LLM_API_TOKEN", "")
    if len(token.strip()) < MIN_TOKEN_LENGTH or token != token.strip():
        raise SystemExit("LLM_API_TOKEN must be set (at least %d characters)" % MIN_TOKEN_LENGTH)
    return token


def load_port(environ=os.environ) -> int:
    raw = environ.get("LLM_API_PORT", "").strip()
    if not raw:
        return DEFAULT_PORT
    try:
        port = int(raw)
    except ValueError:
        raise SystemExit("LLM_API_PORT must be a number")
    if not 1 <= port <= 65535:
        raise SystemExit("LLM_API_PORT must be between 1 and 65535")
    return port


def make_token_check(expected_token: str):
    expected = expected_token.encode("utf-8")

    def check(authorization: Optional[str] = Header(default=None)) -> None:
        scheme, _, value = (authorization or "").partition(" ")
        if scheme != "Bearer" or not value or not hmac.compare_digest(value.encode("utf-8"), expected):
            raise HTTPException(
                status_code=401,
                detail="Unauthorized",
                headers={"WWW-Authenticate": "Bearer"},
            )

    return check


class GenerationRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=8000)
    max_new_tokens: int = Field(default=128, ge=1, le=512)
    temperature: float = Field(default=0.7, ge=0, le=2)
    do_sample: bool = True


class GenerationResponse(BaseModel):
    generated_text: str


def create_app(token: str, generator) -> FastAPI:
    app = FastAPI(
        title="LLM API Server",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
        dependencies=[Depends(make_token_check(token))],
    )

    @app.post("/generate", response_model=GenerationResponse)
    def generate_text(request: GenerationRequest):
        try:
            formatted = (
                "<|system|>\nYou are a helpful assistant.</s>\n"
                "<|user|>\n%s</s>\n<|assistant|>\n" % request.prompt
            )
            results = generator(
                formatted,
                max_new_tokens=request.max_new_tokens,
                do_sample=request.do_sample,
                temperature=request.temperature,
                pad_token_id=generator.tokenizer.eos_token_id,
            )
            reply = results[0]["generated_text"].replace(formatted, "").strip()
            return GenerationResponse(generated_text=reply)
        except Exception:
            logger.exception("Generation failed")
            raise HTTPException(status_code=500, detail="Generation failed")

    @app.get("/health")
    def health_check():
        return {"status": "healthy", "model": MODEL_ID}

    return app


def load_generator():
    import torch
    from transformers import pipeline

    device = 0 if torch.cuda.is_available() else -1
    print("Loading model on %s..." % ("GPU" if device == 0 else "CPU"), flush=True)
    return pipeline(
        "text-generation",
        model=MODEL_ID,
        torch_dtype=torch.float32,
        device=device,
        trust_remote_code=False,
    )


def main() -> None:
    token = load_token()
    port = load_port()
    app = create_app(token, load_generator())
    uvicorn.run(app, host=HOST, port=port)


if __name__ == "__main__":
    main()

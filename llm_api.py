from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from transformers import pipeline
import torch
import uvicorn

# Initialize FastAPI app
app = FastAPI(title="LLM API Server")

# Load a more capable chat model (TinyLlama-1.1B-Chat-v1.0)
# Using device=0 if CUDA is available, else -1 for CPU
device = 0 if torch.cuda.is_available() else -1
print(f"Loading model on {'GPU' if device == 0 else 'CPU'}...")
model_id = "TinyLlama/TinyLlama-1.1B-Chat-v1.0"
generator = pipeline(
    "text-generation", 
    model=model_id, 
    torch_dtype=torch.float32, 
    device=device
)

class GenerationRequest(BaseModel):
    prompt: str
    max_new_tokens: int = 128
    temperature: float = 0.7
    do_sample: bool = True

class GenerationResponse(BaseModel):
    generated_text: str

@app.post("/generate", response_model=GenerationResponse)
async def generate_text(request: GenerationRequest):
    try:
        # Wrap prompt in TinyLlama's chat template
        formatted_prompt = f"<|system|>\nYou are a helpful assistant.</s>\n<|user|>\n{request.prompt}</s>\n<|assistant|>\n"
        
        # Generate text based on prompt
        results = generator(
            formatted_prompt, 
            max_new_tokens=request.max_new_tokens,
            do_sample=request.do_sample,
            temperature=request.temperature,
            pad_token_id=generator.tokenizer.eos_token_id
        )
        
        # Extract the assistant's reply (remove the prompt part)
        generated_text = results[0]['generated_text']
        assistant_reply = generated_text.replace(formatted_prompt, "").strip()
        
        return GenerationResponse(generated_text=assistant_reply)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "healthy", "model": model_id}

if __name__ == "__main__":
    # Run the server on localhost:8000
    uvicorn.run(app, host="0.0.0.0", port=8000)

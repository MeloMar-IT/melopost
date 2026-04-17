# Prerequisites and System Requirements

This document outlines the necessary libraries, databases, and configurations required to run Melopost and its RAG (Retrieval Augmented Generation) system on different operating systems.

## 🐍 Python Requirements

The RAG system requires Python 3.9 or higher.

### Required Libraries

Install the following libraries using `pip`:

```bash
pip install pandas qdrant-client sentence-transformers torch transformers PyPDF2 langchain-text-splitters requests
```

### OS-Specific Python Setup

#### 🍏 macOS
1. Install Python via [Homebrew](https://brew.sh/):
   ```bash
   brew install python
   ```
2. (Optional) Create a virtual environment:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

#### 🪟 Windows
1. Download and install Python from [python.org](https://www.python.org/downloads/windows/). Ensure "Add Python to PATH" is checked.
2. (Optional) Create a virtual environment:
   ```bash
   python -m venv venv
   .\venv\Scripts\activate
   ```

#### 🐧 Linux (Ubuntu/Debian)
1. Install Python:
   ```bash
   sudo apt update
   sudo apt install python3 python3-pip python3-venv
   ```
2. (Optional) Create a virtual environment:
   ```bash
   python3 -m venv venv
   source venv/bin/activate
   ```

---

## 🗄️ Database Requirements

Melopost uses **Apache Cassandra** for all primary data storage.

### 1. Cassandra (Core Data)
Cassandra 4.0+ is required for storing structured postmortem data, users, and configurations.

#### 🍏 macOS Setup
1. Install via Homebrew:
   ```bash
   brew install cassandra
   ```
2. Start the service:
   ```bash
   brew services start cassandra
   ```
3. Default keyspace `melopost` will be created automatically on first run.

#### 🪟 Windows Setup
1. It is recommended to run Cassandra using **Docker**:
   ```bash
   docker run --name melopost-cassandra -p 9042:9042 -d cassandra:latest
   ```
2. Alternatively, download the binary from the [Apache Cassandra website](https://cassandra.apache.org/download/), but Docker is highly preferred for Windows stability.

#### 🐧 Linux Setup
1. Add the Apache repository and install:
   ```bash
   echo "deb http://debian.datastax.com/community stable main" | sudo tee -a /etc/apt/sources.list.d/cassandra.sources.list
   curl -L https://debian.datastax.com/debian/repo_key | sudo apt-key add -
   sudo apt update
   sudo apt install cassandra
   ```
2. Enable and start:
   ```bash
   sudo systemctl enable cassandra
   sudo systemctl start cassandra
   ```

### 3. Qdrant (Vector Database for RAG)
The RAG system uses a local Qdrant instance stored in `data/qdrant_db`.
- No separate installation is required if using the `qdrant-client` library with the local path configuration (default).
- If you prefer a server-based Qdrant, you can run it via Docker:
  ```bash
  docker run -p 6333:6333 -p 6334:6334 \
      -v $(pwd)/qdrant_storage:/qdrant/storage:z \
      qdrant/qdrant
  ```

---

## ☕ Java Requirements
- **Java 21** or higher.
- **Maven 3.8+**.

Verify your Java version:
```bash
java -version
```

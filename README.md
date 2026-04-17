# Melopost - Post Mortem Management System

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Melopost is a specialized application designed to manage incident postmortems and facilitate root cause analysis using the **Swiss Cheese Model**. It allows teams to identify layers of defense, find holes in those layers, and track remedial actions to prevent future occurrences.

---

## ✨ Key Features

- **Swiss Cheese Analysis**: Map incidents to 10 standard layers of defense.
- **Timeline Tracking**: Build a chronological view of the incident.
- **Remedial Actions (Stories)**: Link identified "holes" to actionable tasks in Jira, ServiceNow, or Azure DevOps.
- **Reporting**: Generate professional PDF reports using customizable Mustache templates.
- **Document Management**: Securely store and manage incident-related logs, screenshots, and diagrams.
- **Guiding Questions**: Pre-defined questions for each layer to assist in analysis.

---

## 📖 Documentation

For a comprehensive guide on how to use Melopost, please refer to the [**User Guide**](docs/USER_GUIDE.md).
For setup requirements and dependencies, see the [**Prerequisites Guide**](docs/PREREQUISITES.md).

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.4.4
- **Security**: Spring Security (Role-based access control)
- **Database**: Cassandra 4.0+
- **Persistence**: Spring Data Cassandra
- **Templating**: Thymeleaf (Web UI) and Mustache (Reports)
- **PDF Generation**: OpenHTMLToPDF
- **Utilities**: Lombok

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or higher.
- **Maven 3.8+**.
- **Cassandra 4.0+** (running as a local service).

### Building and Running
1. **Clone the repository**:
   ```bash
   git clone https://dev.azure.com/INGCDaaS/IngOne/_git/P32377-PSSSRE-POSTMORTEM-MANAGEMENT
   cd melopost
   ```
2. **Ensure Cassandra is running**:
   - Make sure your local Cassandra service is started (e.g., `brew services start cassandra` on macOS).
   - Keyspace `melopost` will be created automatically on first run.
3. **Build the project**:
   ```bash
   ./mvnw clean install
   ```
4. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
5. **Access the application**:
   - Web Interface: `http://localhost:8080`
   - Default Credentials: `admin` / `admin`

### Running with Different Environments (DTAP)
Melopost supports different configurations for Development, Testing, Acceptance, and Production environments using Spring Profiles.

1. **Development (Default)**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
2. **Testing**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=test
   ```
3. **Acceptance**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=acc
   ```
4. **Production**:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
   ```

Note: For **Acceptance** and **Production**, you should provide Cassandra connection details via environment variables:
- `SPRING_CASSANDRA_CONTACT_POINTS`
- `SPRING_CASSANDRA_LOCAL_DATACENTER`
- `SPRING_CASSANDRA_PORT` (optional, defaults to 9042)
- `SPRING_CASSANDRA_KEYSPACE_NAME`

### Database Management
Melopost uses Cassandra for all its data. You can interact with it using `cqlsh`.
- **Keyspace**: `melopost` (default)
- **Default Contact Point**: `127.0.0.1:9042`

---

## 🔌 API Overview

Melopost provides a RESTful API for all core entities. All endpoints require authentication.

| Category | Base Endpoint | Description |
| :--- | :--- | :--- |
| **Postmortems** | `/api/postmortems` | Create, read, update, and delete incident analyses. |
| **Stories** | `/api/stories` | Manage remedial action items. |
| **Data Sources** | `/api/datasources` | Configure external system integrations. |
| **Templates** | `/api/report-templates` | Manage Mustache PDF templates. |
| **Users** | `/api/users` | Administrative user management. |

---

## 📄 Mustache Templates

Melopost supports customizable Mustache templates for PDF reports. You can define your own templates to match your organization's branding and reporting requirements.

Refer to the [**Mustache Template Documentation**](docs/MUSTACHE_TEMPLATES.md) for a full list of available variables and sections.

---

*Refer to the [API Documentation](docs/API.md) and [Database Schema](docs/DATABASE.md) for detailed specifications.*

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for full details.

---

## 🤖 RAG (Retrieval Augmented Generation)

You can create a local vector database for RAG based on the existing postmortem data in Cassandra, local documentation, and exported reports.

### 1. Requirements
Ensure you have Python installed and the required libraries:
```bash
pip install pandas qdrant-client sentence-transformers torch transformers PyPDF2 langchain-text-splitters
```

### 2. Configuration
The RAG system uses local models stored in the `model/` directory. Ensure the following models are available:
- **Embedding Model**: `model/Qwen3-Embedding-0.6B`
- **LLM Model**: `model/Qwen3.5-0.8B`

### 3. Ingestion
#### Download and ingest all postmortem reports
You can also download all postmortem reports as PDF from the application and ingest them directly into the RAG:
```bash
python python_scripts/download_all_postmortems.py
```
This script will fetch all postmortems via the API, generate their PDF reports, and ingest them into the vector database.

**Note:** Ensure the Java application is running or the Cassandra database is accessible via the API (defaults to `http://localhost:8080/api`).

The vector database will be created in the `data/qdrant_db` directory.

### 4. Chatting with RAG
Once the database is populated, you can interact with it using the chat script:
```bash
python python_scripts/chat_with_rag.py
```
This script uses the local Qwen model to answer questions based on the retrieved context from your postmortems and documents.

---

---
## To-Do
- [X] Add frontend for connecting local to Major
- [X] Switch databases to Cassandra
- [X] Add filtering to the postmortem list
- [X] Add state to the postmortem
- [X] Add a new hole overview page
- [X] Add a new story overview page
- [X] make the configuration production ready
- [X] Add a new incident notes to the application as replacement of Loop.
- [X] Change all Note parts to a MD format Note.
- [ ] Updated Parsing of documents into a Postmortem record
- [ ] Hardware choose
- [X] Create a graphical timeline for the postmortem
- [ ] Docker container support
- [ ] Deployment pipeline
- [ ] Figure out how to deploy/store Model for fuzzy logic
- [ ] Make the logic for duplicate holes/story fuzzy
- [ ] Create logic to find systemic Causes
- [ ] Extend reporting
  - [ ] Systemic Cause per department
  - [ ] Reporting per per department over time.
  - [ ] Reporting per application
- [ ] Access to SNOW
- [ ] Access to DEV/OPS
- [ ] Register as a Application
- [ ] Code scan
- [ ] Risk evaluation ??????
- [ ] Fact finding ???????????
- [ ] Add role based access control / single sign-on
- [ ] Teams integration so that you can import the text from the TEAMS chat.
- [ ] Can we download the deparrtments from somewhere and show those. ( ACE )
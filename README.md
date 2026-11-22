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
- **Security**: Spring Security (Role-based access control), a Content-Security-Policy header, and an allow-list HTML sanitizer for rich text (hole descriptions and remedial actions) applied when pages are rendered
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
   - Login: user `admin`, password from the `MELOPOST_ADMIN_INITIAL_PASSWORD` environment variable (see [Startup Behaviour](#startup-behaviour)). If that variable is not set, no admin is created and you need to set it before the first start.

### Running the tests
The test suite has two tiers:
- **`./mvnw test`** runs the unit and web-slice tests (`*Test`). It needs neither Cassandra nor Docker.
- **`./mvnw verify`** also runs the integration tests (`*IT`) and the `*FunctionalTest` classes against a `cassandra:4.1` container started by Testcontainers. Docker must be running. Without Docker, those classes are skipped.

`./mvnw clean install` runs the same phases as `verify`, so it also needs Docker for the Cassandra tests.

CI (`.github/workflows/ci.yml`) runs `./mvnw -B verify` on every push and pull request. It requires Docker on the runner, and it fails if the Cassandra test classes report any skipped test. The surefire and failsafe reports are uploaded as the `test-reports` artifact.

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
- `MELOPOST_ADMIN_INITIAL_PASSWORD` (only needed on the first start, when no user exists yet)

### Startup Behaviour
On every start, Melopost keeps existing data. It never deletes postmortems, data sources, incident notes or users.

- **Report templates**: the built-in templates are created or updated in every profile.
- **First admin**: if the `users` table is empty and `MELOPOST_ADMIN_INITIAL_PASSWORD` is set, an `admin` user is created with that password (roles `ADMIN` and `USER`). The password must be at least 12 characters. If it is missing, too short, or users already exist, no admin is created and a message is logged. The password is never logged.
- **Sample data**: demo users (`admin`/`admin123`, `user1`, `user2`) and sample postmortems are created only with `melopost.seed.enabled=true` under the `dev` or `test` profile. The seeder never runs under `prod` or `acc`, and it does nothing if postmortems already exist. Its demo passwords are well known: never enable it on a shared instance.

Example for a first start in development with sample data:
```bash
MELOPOST_ADMIN_INITIAL_PASSWORD='choose-a-long-password' \
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--melopost.seed.enabled=true
```

### Database Management
Melopost uses Cassandra for all its data. You can interact with it using `cqlsh`.
- **Keyspace**: `melopost` (default)
- **Default Contact Point**: `127.0.0.1:9042`

---

## 🔌 API Overview

Melopost provides a RESTful API for all core entities. All endpoints require authentication. Non-admin users are limited to their own departments, and Published postmortems are locked for them; see [Access Control](docs/API.md#access-control).

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

## 🤖 Optional Local LLM Helper

Melopost can start a small local LLM helper (`llm_api.py`) as a child process. It is **off by default**, so the application starts without Python installed.

The former RAG scripts in `python_scripts/` have been removed. They indexed all departments' postmortems as `admin`, which bypassed department access, so they are no longer supported.

### 1. Requirements
Python 3.9 or higher, then the libraries in `requirements.txt`:
```bash
pip install -r requirements.txt
```

### 2. Enable the helper
1. Generate a token of at least 32 characters and provide it through the environment:
   ```bash
   export MELOPOST_LLM_API_TOKEN="$(openssl rand -base64 32)"
   ```
2. Start Melopost with `melopost.llm-api.enabled=true`.

Properties:
- `melopost.llm-api.enabled` (default `false`): starts the helper when `true`.
- `melopost.llm-api.port` (default `8000`): port the helper listens on.
- `melopost.llm-api.python-executable` (default `python3`) and `melopost.llm-api.script-path` (default `llm_api.py`).

### 3. Security
- The helper listens on `127.0.0.1` only. It cannot be reached from another host.
- Every endpoint requires `Authorization: Bearer <token>`. Requests without a valid token get `401 Unauthorized`.
- The token is read only from the `MELOPOST_LLM_API_TOKEN` environment variable. A value in a property file is ignored. If the variable is unset or blank, Melopost generates a random token for each start. Only the application can then use the helper.
- The token is passed to the helper through its environment, not on the command line.
- The `/docs`, `/redoc` and `/openapi.json` pages are disabled.
- Input is limited: prompt up to 8000 characters, `max_new_tokens` from 1 to 512, `temperature` from 0 to 2. Errors return a generic message; details are only logged on the server.
- The model (`TinyLlama/TinyLlama-1.1B-Chat-v1.0`) is loaded with `trust_remote_code=False`.

### 4. Manual check
With the helper running, from the repository root:
```bash
LLM_API_TOKEN="$MELOPOST_LLM_API_TOKEN" python test_llm_api.py
```
The Python tests can be run with `python -m unittest discover -s tests/python`.

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
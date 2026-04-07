# Melopost - Post Mortem Management System

Melopost is a specialized application designed to manage incident postmortems and facilitate root cause analysis using the **Swiss Cheese Model**. It allows teams to identify layers of defense, find holes in those layers, and track remedial actions to prevent future occurrences.

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the `LICENSE` file for full details.

## 🗄️ Database Structure

The application uses an H2 relational database. The core entities and their relationships are:

- **Postmortem**: The primary entity representing an incident analysis.
  - Contains: title, description, department, failed application, incident date, start date, due date, meeting date, incident reference, incident source, story application, and tags.
  - Relationships: 1-to-Many with `CheeseLayer`, 1-to-Many with `TimelineEvent`, 1-to-Many with `PostmortemQuestion`, 1-to-Many with `PostmortemDocument`.
- **CheeseLayer**: Represents a specific layer of defense (e.g., Software Design, Training, Monitoring).
  - Contains: name, description.
  - Relationships: Belongs to `Postmortem`, 1-to-Many with `Hole`.
- **Hole**: Represents a failure or weakness discovered in a layer.
  - Contains: description, team name, remedial action, action status, and tags.
  - Relationships: Belongs to `CheeseLayer`, 1-to-1 with `Story`.
- **Story**: An action item created in an external system (Jira/ServiceNow/Azure DevOps).
  - Contains: story number, team name, backlog name, platform, what to fix, found by department, to solve by department, priority, manager name, status, notes, external links, and tags.
  - Relationships: Belongs to `Hole`.
- **TimelineEvent**: Key chronological events that occurred during the incident.
  - Contains: event time, description.
  - Relationships: Belongs to `Postmortem`.
- **PostmortemQuestion**: Represents a guiding question for the postmortem analysis.
  - Contains: question, answer, cheese layer.
  - Relationships: Belongs to `Postmortem`.
- **PostmortemDocument**: An uploaded file related to the postmortem.
  - Contains: file name, content type, size, upload date.
  - Relationships: Belongs to `Postmortem`.
- **ReportTemplate**: A template for generating PDF reports.
  - Contains: name, content (Mustache template), is default.
- **DataSource**: Configuration for external incident and story management systems.
  - Contains: name, type (Jira, ServiceNow, Azure DevOps), operation (Read, Create), URL, username, password, and description.
- **User**: Application users.
  - Contains: username, password (encrypted), email, first name, last name, roles (ADMIN, USER), and status.

## 🔌 APIs

The application provides a RESTful API for integration and programmatic access. All API endpoints require authentication.

### Postmortems
- `GET /api/postmortems`: Retrieve all postmortems.
- `GET /api/postmortems/recent`: Retrieve recent postmortems.
- `GET /api/postmortems/{id}`: Get a specific postmortem by ID.
- `POST /api/postmortems`: Create a new postmortem.
- `PUT /api/postmortems/{id}`: Update an existing postmortem.
- `DELETE /api/postmortems/{id}`: Delete a postmortem.
- `GET /api/postmortems/search?keyword={keyword}`: Search for postmortems by keyword.
- `GET /api/postmortems/{id}/report`: Download a PDF report for a postmortem.

### Data Sources
- `GET /api/datasources`: Retrieve all data sources.
- `GET /api/datasources/{id}`: Get a specific data source.
- `POST /api/datasources`: Create a new data source.
- `PUT /api/datasources/{id}`: Update a data source.
- `DELETE /api/datasources/{id}`: Delete a data source.
- `GET /api/datasources/templates`: List data source templates.

### Stories
- `GET /api/stories`: List all stories.
- `GET /api/stories/{id}`: Get a specific story.
- `POST /api/stories`: Create a story.
- `PUT /api/stories/{id}`: Update a story.
- `DELETE /api/stories/{id}`: Delete a story.

### Cheese Layers
- `GET /api/cheese-layers`: List all cheese layers.
- `GET /api/cheese-layers/{id}`: Get a specific cheese layer.
- `POST /api/cheese-layers`: Create a cheese layer.
- `PUT /api/cheese-layers/{id}`: Update a cheese layer.
- `DELETE /api/cheese-layers/{id}`: Delete a cheese layer.

### Holes
- `GET /api/holes`: List all holes.
- `GET /api/holes/{id}`: Get a specific hole.
- `POST /api/holes`: Create a hole.
- `PUT /api/holes/{id}`: Update a hole.
- `DELETE /api/holes/{id}`: Delete a hole.

### Timeline Events
- `GET /api/timeline-events`: List all timeline events.
- `GET /api/timeline-events/{id}`: Get a specific event.
- `POST /api/timeline-events`: Create an event.
- `PUT /api/timeline-events/{id}`: Update an event.
- `DELETE /api/timeline-events/{id}`: Delete an event.

### Report Templates
- `GET /api/report-templates`: List all report templates.
- `GET /api/report-templates/{id}`: Get a specific template.
- `GET /api/report-templates/default`: Get the default template.
- `POST /api/report-templates`: Create a new template.
- `PUT /api/report-templates/{id}`: Update a template.
- `DELETE /api/report-templates/{id}`: Delete a template.

### Postmortem Questions
- `GET /api/postmortem-questions`: List all questions.
- `GET /api/postmortem-questions/{id}`: Get a specific question.
- `GET /api/postmortem-questions/postmortem/{id}`: List questions for a postmortem.
- `POST /api/postmortem-questions`: Create a question.
- `PUT /api/postmortem-questions/{id}`: Update a question.
- `DELETE /api/postmortem-questions/{id}`: Delete a question.

### Postmortem Documents
- `GET /api/postmortem-documents`: List all documents.
- `GET /api/postmortem-documents/{id}`: Get a specific document.
- `GET /api/postmortem-documents/postmortem/{id}`: List documents for a postmortem.
- `GET /api/postmortem-documents/{id}/download`: Download a document.
- `POST /api/postmortem-documents/upload/{postmortemId}`: Upload a document.
- `DELETE /api/postmortem-documents/{id}`: Delete a document.

### Users
- `GET /api/users`: List all users.
- `GET /api/users/{id}`: Get a specific user.
- `POST /api/users`: Create a new user.
- `PUT /api/users/{id}`: Update a user.
- `DELETE /api/users/{id}`: Delete a user.

## 🚀 How to Use

### Functional Usage
1. **Create a Postmortem**: Start by recording an incident. The system automatically initializes a set of "Cheese Layers" (Define, Design, Build, Test, Release, Run, Resilience, Observability, Incident Handling, Human) to guide your analysis.
2. **Identify Holes**: For each layer, identify what failed (the "hole").
3. **Plan Remedial Actions**: Create "Stories" for each hole to ensure the gap is closed in your backlog.
4. **Track Timeline**: Add key events to build a chronological view of the incident.
5. **Guiding Questions**: Answer predefined questions for each layer to help identify root causes.
6. **Document Management**: Upload and manage relevant documents (e.g., log files, screenshots) for each postmortem.
7. **Report Generation**: Generate professional PDF reports based on your analysis and templates.
8. **Tagging**: Use tags to categorize incidents, holes, and stories for better searching and reporting.

### Technical Usage

#### Prerequisites
- **Java 21** or higher.
- **Maven 3.8+**.

#### Building and Running
1. **Build the project**:
   ```bash
   ./mvnw clean install
   ```
2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Access the application**:
   - Web Interface: `http://localhost:8080`
   - H2 Console: `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:file:./data/melopost`
     - Username: `sa`
     - Password: (blank)

#### Default Credentials
The application is initialized with a default administrator account:
- **Username**: `admin`
- **Password**: `admin`

## 🛠️ Tech Stack
- **Framework**: Spring Boot 3.4.4
- **Security**: Spring Security
- **Database**: H2 (In-memory/File-based)
- **Persistence**: Spring Data JPA / Hibernate
- **Templating**: Thymeleaf (Web UI) and Mustache (Reports)
- **PDF Generation**: OpenHTMLToPDF
- **Utilities**: Lombok

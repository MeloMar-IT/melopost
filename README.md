# Melopost - Post Mortem Management System

Melopost is a specialized application designed to manage incident postmortems and facilitate root cause analysis using the **Swiss Cheese Model**. It allows teams to identify layers of defense, find holes in those layers, and track remedial actions to prevent future occurrences.

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the `LICENSE` file for full details.

## 🗄️ Database Structure

The application uses an H2 relational database. The core entities and their relationships are:

- **Postmortem**: The primary entity representing an incident analysis.
  - Contains: title, description, department, failed application, incident date, start date, due date, meeting date, incident reference, and tags.
  - Relationships: 1-to-Many with `CheeseLayer`, 1-to-Many with `TimelineEvent`.
- **CheeseLayer**: Represents a specific layer of defense (e.g., Software Design, Training, Monitoring).
  - Contains: name, description.
  - Relationships: Belongs to `Postmortem`, 1-to-Many with `Hole`.
- **Hole**: Represents a failure or weakness discovered in a layer.
  - Contains: description, team name, remedial action, action status, and tags.
  - Relationships: Belongs to `CheeseLayer`, 1-to-1 with `Story`.
- **Story**: An action item created in an external system (Jira/ServiceNow).
  - Contains: story number, team name, backlog name, priority, status, manager, and external links.
  - Relationships: Belongs to `Hole`.
- **TimelineEvent**: Key chronological events that occurred during the incident.
  - Contains: event time, description.
  - Relationships: Belongs to `Postmortem`.
- **DataSource**: Configuration for external incident and story management systems.
  - Contains: name, type (Jira, ServiceNow, Azure DevOps), operation (Read, Create), and URL.
- **User**: Application users.
  - Contains: username, password (encrypted), email, roles (ADMIN, USER), and status.

## 🔌 APIs

The application provides a RESTful API for integration and programmatic access. All API endpoints require authentication.

### Postmortems
- `GET /api/postmortems`: Retrieve all postmortems.
- `GET /api/postmortems/{id}`: Get a specific postmortem by ID.
- `POST /api/postmortems`: Create a new postmortem.
- `PUT /api/postmortems/{id}`: Update an existing postmortem.
- `DELETE /api/postmortems/{id}`: Delete a postmortem.
- `GET /api/postmortems/search?keyword={keyword}`: Search for postmortems by keyword.
- `GET /postmortems/{id}/report`: Download a PDF report for a postmortem.

### Data Sources
- `GET /api/datasources`: Retrieve all data sources.
- `POST /api/datasources`: Create a new data source.
- `DELETE /api/datasources/{id}`: Delete a data source.

### Users
- `GET /api/users`: List all users.
- `POST /api/users`: Create a new user.

## 🚀 How to Use

### Functional Usage
1. **Create a Postmortem**: Start by recording an incident. The system automatically initializes a set of "Cheese Layers" (Define, Design, Build, Test, Release, Run, Resilience, Observability, Incident Handling, Human) to guide your analysis.
2. **Identify Holes**: For each layer, identify what failed (the "hole").
3. **Plan Remedial Actions**: Create "Stories" for each hole to ensure the gap is closed in your backlog.
4. **Track Timeline**: Add key events to build a chronological view of the incident.
5. **Tagging**: Use tags to categorize incidents and holes for better searching and reporting.

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
- **Templating**: Thymeleaf with Layout Dialect
- **Utilities**: Lombok

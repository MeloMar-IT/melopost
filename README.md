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

For a comprehensive guide on how to use Melopost, please refer to the [**User Guide**](USER_GUIDE.md).

---

## 🛠️ Tech Stack

- **Framework**: Spring Boot 3.4.4
- **Security**: Spring Security (Role-based access control)
- **Database**: H2 (In-memory/File-based)
- **Persistence**: Spring Data JPA / Hibernate
- **Templating**: Thymeleaf (Web UI) and Mustache (Reports)
- **PDF Generation**: OpenHTMLToPDF
- **Utilities**: Lombok

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or higher.
- **Maven 3.8+**.

### Building and Running
1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-repo/melopost.git
   cd melopost
   ```
2. **Build the project**:
   ```bash
   ./mvnw clean install
   ```
3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```
4. **Access the application**:
   - Web Interface: `http://localhost:8080`
   - Default Credentials: `admin` / `admin`

### Database Management
- **H2 Console**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:file:./data/melopost`
- **Username**: `sa` | **Password**: (blank)

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

*Refer to the [API Documentation](API.md) (coming soon) for detailed endpoint specifications.*

---

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for full details.

# Melopost API Documentation

Melopost provides a RESTful API for all core entities. All endpoints require authentication.

## Table of Contents
1. [Postmortems](#postmortems)
2. [Stories](#stories)
3. [Data Sources](#data-sources)
4. [Report Templates](#report-templates)
5. [Users](#users)

---

## Postmortems

Base Endpoint: `/api/postmortems`

### Get All Postmortems
- **Method**: `GET`
- **Response**: List of Postmortem objects.

### Create Postmortem
- **Method**: `POST`
- **Body**: `Postmortem` object.
- **Example JSON Request**:
```json
{
  "title": "ServiceNow Incident #305",
  "description": "Critical failure in user authentication.",
  "incidentDate": "2026-03-21T09:50:00",
  "startDate": "2026-03-21T06:50:00",
  "postMortemMeetingDate": "2026-04-01T09:50:00",
  "incidentRef": "S-1365",
  "incidentSource": "ServiceNow",
  "storyStore": "Service NOW read",
  "department": "Operations",
  "failedApplication": "User Authentication",
  "type": "Orchestrated P1",
  "status": "In Analysis",
  "tags": ["ui", "auth"],
  "layers": [
    {
      "name": "Define",
      "holes": [
        {
          "description": "Root cause discovered in the define layer.",
          "teamName": "DevOps",
          "remedialAction": "Update documentation.",
          "actionStatus": "PENDING"
        }
      ]
    }
  ],
  "timelineEvents": [
    {
      "eventTime": "2026-03-21T09:50:00",
      "description": "Event 1: Progress during incident handling."
    }
  ]
}
```

### Update Postmortem
- **Method**: `PUT`
- **Path**: `/api/postmortems/{id}`
- **Body**: `Postmortem` object (partial updates supported).

### Delete Postmortem
- **Method**: `DELETE`
- **Path**: `/api/postmortems/{id}`

### Download PDF Report
- **Method**: `GET`
- **Path**: `/api/postmortems/{id}/report`
- **Response**: PDF File.

---

## Stories

Base Endpoint: `/api/stories`

### Get All Stories
- **Method**: `GET`

### Create Story
- **Method**: `POST`
- **Example JSON Request**:
```json
{
  "storyNumber": "STR-1001",
  "teamName": "Identity Team",
  "backlogName": "Product Backlog",
  "platform": "Jira",
  "whatToFix": "Fix token expiration bug",
  "priority": "High",
  "status": "In Progress"
}
```

---

## Data Sources

Base Endpoint: `/api/datasources`

### Get All Data Sources
- **Method**: `GET`

### Create Data Source
- **Method**: `POST`
- **Example JSON Request**:
```json
{
  "name": "Production Jira",
  "type": "Jira",
  "operation": "Create",
  "url": "https://jira.example.com",
  "username": "api-user",
  "password": "secure-password",
  "description": "Main Jira instance for creating remedial action stories."
}
```

---

## Report Templates

Base Endpoint: `/api/report-templates` (handled via `ReportTemplateViewController` for UI and potentially internal API).

*Note: Administrative UI for managing templates is available at `/templates`.*

---

## Users

Base Endpoint: `/api/users`

### Get All Users
- **Method**: `GET`

### Create/Update User
- **Method**: `POST`
- **Example JSON Request**:
```json
{
  "username": "jdoe",
  "password": "password123",
  "email": "jdoe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ROLE_USER"],
  "allowedDepartments": ["IT", "Operations"]
}
```

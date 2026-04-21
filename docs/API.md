# Melopost API Documentation

Melopost provides a RESTful API for all core entities. All endpoints require authentication.
Administrative endpoints are restricted to users with the `ROLE_ADMIN` role.

## Table of Contents
1. [Postmortems](#postmortems)
2. [Postmortem Documents](#postmortem-documents)
3. [Stories](#stories)
4. [Data Sources](#data-sources)
5. [Users](#users)
6. [Database Administration (ADMIN)](#database-administration-admin)

---

## Postmortems

Base Endpoint: `/api/postmortems`

### Get All Postmortems
- **Method**: `GET`
- **Response**: List of `Postmortem` objects.

### Get Recent Postmortems
- **Method**: `GET`
- **Path**: `/api/postmortems/recent`
- **Response**: List of the 5 most recent `Postmortem` objects.

### Get Postmortem by UUID
- **Method**: `GET`
- **Path**: `/api/postmortems/{uuid}`
- **Response**: `Postmortem` object.

### Search Postmortems
- **Method**: `GET`
- **Path**: `/api/postmortems/search`
- **Query Parameter**: `keyword` (String) - Search in title, reference, description, and tags.
- **Response**: List of `PostmortemSearchResultDTO` objects.

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
  ],
  "questions": [
    {
      "question": "Was the monitoring effective?",
      "answer": "Yes, it triggered immediately."
    }
  ]
}
```

### Import Postmortem from Document
- **Method**: `POST`
- **Path**: `/api/postmortems/import`
- **Form Data**: `file` (MultipartFile) - Supported formats: .docx, .pdf, .txt.
- **Response**: `Postmortem` object parsed from the document.

### Update Postmortem
- **Method**: `PUT`
- **Path**: `/api/postmortems/{uuid}`
- **Body**: `Postmortem` object (partial updates supported).

### Delete Postmortem
- **Method**: `DELETE`
- **Path**: `/api/postmortems/{uuid}`

### Download PDF Report
- **Method**: `GET`
- **Path**: `/api/postmortems/{uuid}/report`
- **Response**: PDF File stream.

---

## Postmortem Documents

Base Endpoint: `/api/postmortem-documents`

### Get All Documents
- **Method**: `GET`
- **Response**: List of `PostmortemDocument` metadata.

### Get Document by UUID
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/{uuid}`
- **Response**: `PostmortemDocument` metadata.

### Get Documents for Postmortem
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/postmortem/{postmortemUuid}`
- **Response**: List of `PostmortemDocument` metadata for the specified postmortem.

### Download Document
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/{uuid}/download`
- **Response**: File stream.

### Upload Document
- **Method**: `POST`
- **Path**: `/api/postmortem-documents/upload/{postmortemUuid}`
- **Form Data**: `file` (MultipartFile)
- **Response**: The created `PostmortemDocument` metadata.

### Delete Document
- **Method**: `DELETE`
- **Path**: `/api/postmortem-documents/{uuid}`

---

## Stories

Base Endpoint: `/api/stories`

Note: Stories are currently managed as part of Postmortem Holes.

---

## Data Sources

Base Endpoint: `/api/datasources`

### Get All Data Sources
- **Method**: `GET`
- **Response**: List of `DataSource` objects.

### Get Data Source by UUID
- **Method**: `GET`
- **Path**: `/api/datasources/{uuid}`
- **Response**: `DataSource` object.

### Get Template Data Sources
- **Method**: `GET`
- **Path**: `/api/datasources/templates`
- **Response**: List of pre-defined `DataSource` templates.

### Create Data Source
- **Method**: `POST`
- **Example JSON Request**:
```json
{
  "name": "Production Jira",
  "type": "Jira",
  "operation": "CREATE_STORY",
  "url": "https://jira.example.com",
  "username": "api-user",
  "password": "secure-password",
  "description": "Main Jira instance for creating remedial action stories."
}
```

### Update Data Source
- **Method**: `PUT`
- **Path**: `/api/datasources/{uuid}`

### Delete Data Source
- **Method**: `DELETE`
- **Path**: `/api/datasources/{uuid}`

---

## Users

Base Endpoint: `/api/users`

### Get All Users
- **Method**: `GET`
- **Response**: List of `User` objects.

### Get User by UUID
- **Method**: `GET`
- **Path**: `/api/users/{uuid}`
- **Response**: `User` object.

### Create User
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
  "active": true
}
```

### Update User
- **Method**: `PUT`
- **Path**: `/api/users/{uuid}`

### Delete User
- **Method**: `DELETE`
- **Path**: `/api/users/{uuid}`

---

## Database Administration (ADMIN)

Base Endpoint: `/api/admin/database`

### List Tables and Types
- **Method**: `GET`
- **Path**: `/api/admin/database/tables`
- **Query Parameter**: `includeCounts` (boolean, default: true)
- **Response**: List of `DatabaseTableDTO` containing names, types (TABLE/TYPE), row counts, and column names.

### Execute Raw Query
- **Method**: `POST`
- **Path**: `/api/admin/database/query`
- **Body**: `{"query": "SELECT * FROM postmortem"}`
- **Response**: `QueryResultDTO` containing results and metadata.

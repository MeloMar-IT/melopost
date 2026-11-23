# Melopost API Documentation

Melopost provides a RESTful API for all core entities. All endpoints require authentication.
Administrative endpoints are restricted to users with the `ROLE_ADMIN` role.

### Access Control

Every postmortem, and every document attached to one, is owned by its **department**. The rules apply to the REST API and to the web UI alike:

- **Department scope**: a non-admin user only reaches postmortems whose `department` is in the user's allowed departments. Other postmortems return `403 Forbidden`. Lists (`GET /api/postmortems`, `/recent`, `/search`, `/api/postmortem-documents`) silently leave out postmortems and documents from other departments. A postmortem without a department is visible to admins only.
- **Published lock**: a non-admin user cannot update or delete a postmortem whose `status` is `Published`. The API returns `403 Forbidden`. Admins are not affected.
- **Admins** (`ROLE_ADMIN`) are not restricted by department or by the lock.
- **Moving a postmortem**: changing `department` requires the user to be allowed to create postmortems in the new department as well.
- **Linked postmortems**: a non-admin can only link postmortems they are allowed to see.
- **Documents**: access to a document is decided by the postmortem it belongs to. Reading needs view access, uploading, and deleting need update access. A document that does not belong to the postmortem named in the URL returns `404 Not Found`. Documents without a postmortem are visible to admins only.

| Status | Meaning |
| :--- | :--- |
| `403 Forbidden` | The user is not allowed to access the department, or the postmortem is Published and the user is not an admin. |
| `404 Not Found` | The postmortem or document does not exist, or the document is not part of the given postmortem. |
| `409 Conflict` | Another postmortem already uses the same `incidentRef`. |

### CSRF Protection

Every state-changing request (`POST`, `PUT`, `DELETE`) must carry a valid CSRF token, whether the caller is logged in through the browser session or sends HTTP Basic credentials. Requests without a valid token return `403 Forbidden` and change nothing. `GET` requests never change data.

- **Browser UI**: the pages include the token in `<meta name="_csrf">` and the header name in `<meta name="_csrf_header">`. Forms and `fetch` calls in the UI send it automatically.
- **Scripts and other clients**: load a page of the web UI in the same session, read the token from the `_csrf` meta tag and the header name from `_csrf_header` (`X-CSRF-TOKEN` by default), and send that header with each write.
- **Web UI delete actions** (users, incident notes, templates, data sources) are `POST` forms. A `GET` to a `/delete/{id}` URL returns `405 Method Not Allowed` and deletes nothing.

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
- **Response**: List of `Postmortem` objects the caller may access (see [Access Control](#access-control)).

### Get Recent Postmortems
- **Method**: `GET`
- **Path**: `/api/postmortems/recent`
- **Response**: List of the 5 most recent `Postmortem` objects the caller may access.

### Get Postmortem by UUID
- **Method**: `GET`
- **Path**: `/api/postmortems/{uuid}`
- **Response**: `Postmortem` object.
- **Errors**: `403` if the postmortem belongs to another department; `404` if it does not exist.

### Search Postmortems
- **Method**: `GET`
- **Path**: `/api/postmortems/search`
- **Query Parameter**: `keyword` (String) - Search in title, reference, description, and tags. Only postmortems the caller may access are returned.
- **Response**: List of `PostmortemSearchResultDTO` objects.

### Create Postmortem
- **Method**: `POST`
- **Body**: `Postmortem` object. Any `uuid` in the body is ignored: the server always assigns a new one, so an existing postmortem cannot be overwritten by creating one with its UUID.
- **Errors**: `403` if the caller is not allowed to create postmortems in the given `department`; `409` if the `incidentRef` is already in use.
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
- **Errors**: `403` or `409` as for create. The imported postmortem is not stored when either applies.

### Update Postmortem
- **Method**: `PUT`
- **Path**: `/api/postmortems/{uuid}`
- **Body**: `Postmortem` object (partial updates supported).
- **Errors**: `403` if the postmortem belongs to another department, or if it is `Published` and the caller is not an admin; `404` if it does not exist.

### Delete Postmortem
- **Method**: `DELETE`
- **Path**: `/api/postmortems/{uuid}`
- **Errors**: `403` if the postmortem belongs to another department, or if it is `Published` and the caller is not an admin; `404` if it does not exist.

### Download PDF Report
- **Method**: `GET`
- **Path**: `/api/postmortems/{uuid}/report`
- **Response**: PDF File stream.
- **Note**: The renderer loads external resources only from an allow-list and never follows redirects. Loopback, private and `file:` URLs load nothing. The Kroki timeline image is off unless configured; see [PDF Reports](../README.md#pdf-reports).

---

## Postmortem Documents

Base Endpoint: `/api/postmortem-documents`

All document routes check access through the owning postmortem (see [Access Control](#access-control)).

### Get All Documents
- **Method**: `GET`
- **Response**: List of `PostmortemDocument` metadata for postmortems the caller may access.

### Get Document by UUID
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/{uuid}`
- **Response**: `PostmortemDocument` metadata.
- **Errors**: `403` if the owning postmortem belongs to another department; `404` if the document does not exist.

### Get Documents for Postmortem
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/postmortem/{postmortemUuid}`
- **Response**: List of `PostmortemDocument` metadata for the specified postmortem.
- **Errors**: `403` or `404` as for `GET /api/postmortems/{uuid}`.

### Download Document
- **Method**: `GET`
- **Path**: `/api/postmortem-documents/{uuid}/download`
- **Response**: File stream, sent as an attachment with `Content-Disposition: attachment; filename="..."; filename*=UTF-8''...`. The `filename*` parameter carries the exact stored name; the quoted `filename` is an ASCII fallback in which quotes and backslashes are escaped.
- **Headers**: `X-Content-Type-Options: nosniff`.
- **Content type**: The stored type when it is one of the allowed types (see [Upload Document](#upload-document)). Otherwise, including for a missing, empty, malformed or wildcard stored type, `application/octet-stream`.
- **Errors**: `403` or `404` as for get by UUID.

### Upload Document
- **Method**: `POST`
- **Path**: `/api/postmortem-documents/upload/{postmortemUuid}`
- **Form Data**: `file` (MultipartFile)
- **Allowed types**: PDF (`.pdf`), DOCX (`.docx`), XLSX (`.xlsx`), PNG (`.png`), JPEG (`.jpg`, `.jpeg`), plain text (`.txt`), CSV (`.csv`). The file name must end in one of these extensions. The declared Content-Type may be empty, `application/octet-stream`, the canonical type for the extension, or a known alias for it (for example `application/csv` for CSV).
- **Stored values**: The file name is reduced to its last path segment. The stored Content-Type is always the canonical type for the extension, not the client's value.
- **Response**: The created `PostmortemDocument` metadata.
- **Errors**: `400 Bad Request` if the file type is not allowed; nothing is stored. `403` if the caller may not update the postmortem (other department, or Published for a non-admin); `404` if the postmortem does not exist.

### Delete Document
- **Method**: `DELETE`
- **Path**: `/api/postmortem-documents/{uuid}`
- **Errors**: `403` if the caller may not update the owning postmortem (other department, or Published for a non-admin); `404` if the document does not exist or is orphaned and the caller is not an admin.

---

## Stories

Base Endpoint: `/api/stories`

Note: Stories are currently managed as part of Postmortem Holes.

---

## Data Sources (ADMIN)

Base Endpoint: `/api/datasources`

All data-source endpoints are restricted to `ROLE_ADMIN`; other users get `403 Forbidden`.

The `password` field (the integration password or token) is write-only. It is accepted in request bodies but never appears in a response, so no response contains a `password` field. Tokens are stored encrypted; see [DATABASE.md](DATABASE.md#5-data_source). On update, leave `password` out or empty to keep the stored token; a non-empty value replaces it.

### Get All Data Sources
- **Method**: `GET`
- **Response**: List of `DataSource` objects, without `password`.

### Get Data Source by UUID
- **Method**: `GET`
- **Path**: `/api/datasources/{uuid}`
- **Response**: `DataSource` object, without `password`.

### Get Template Data Sources
- **Method**: `GET`
- **Path**: `/api/datasources/templates`
- **Response**: List of pre-defined `DataSource` templates.

### Create Data Source
- **Method**: `POST`
- **Example JSON Request** (the response does not repeat `password`):
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

## Users (ADMIN)

Base Endpoint: `/api/users`

The `password` field is write-only. It is accepted in request bodies, but responses never contain it or its bcrypt hash. Every supplied password is bcrypt-encoded on save, including a value that looks like a hash (such as one starting with `$2a$`). On update, leave `password` out or empty to keep the stored hash.

### Get All Users
- **Method**: `GET`
- **Response**: List of `User` objects, without `password`.

### Get User by UUID
- **Method**: `GET`
- **Path**: `/api/users/{uuid}`
- **Response**: `User` object, without `password`.

### Create User
- **Method**: `POST`
- **Example JSON Request** (the response does not repeat `password`):
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

### Get Table or Type Details
- **Method**: `GET`
- **Path**: `/api/admin/database/table/{name}`
- **Response**: `TableInfoDTO` with the table's columns and a preview of its first 50 rows. The preview is built from the schema, so no query text is accepted from the caller. A column named `password` is returned as `***`. The web UI shows the returned values as plain text.

> The raw CQL query endpoint (`POST /api/admin/database/query`) has been removed. Use `cqlsh` for ad-hoc queries.

### Rich Text Fields
Hole descriptions and remedial actions are stored exactly as submitted. The API returns them unchanged. The web UI cleans them with an allow-list sanitizer before rendering, so scripts, event-handler attributes and `javascript:` links are removed on display. API clients that render these fields as HTML must apply their own sanitizing.

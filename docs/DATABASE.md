# Melopost Database Schema

Melopost uses **Apache Cassandra** for storing its data. This document describes the tables, User-Defined Types (UDTs), and the data model.

## Data Model Overview

The system is designed around the `postmortem` table. Unlike traditional relational databases, Melopost uses a "flat" structure where related entities like timeline events, defense layers, and holes are stored as nested collections within the postmortem record using Cassandra UDTs.

---

## Tables

### 1. `postmortem`
The primary table for incident analysis.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key (Internal ID) |
| `title` | TEXT | Title of the postmortem |
| `description` | TEXT | Brief summary |
| `incident_date` | TIMESTAMP | When the incident occurred |
| `start_date` | TIMESTAMP | When investigation started |
| `post_mortem_meeting_date` | TIMESTAMP | When the meeting was held |
| `due_date` | TIMESTAMP | Target completion date |
| `incident_ref` | TEXT | External reference ID (e.g., ServiceNow ID) |
| `incident_source` | TEXT | Source system (e.g., "ServiceNow") |
| `story_store` | TEXT | Where remedial actions are tracked |
| `department` | TEXT | Responsible department |
| `failed_application` | TEXT | The application that failed |
| `type` | TEXT | Classification (e.g., "Major Postmortem") |
| `status` | TEXT | Current state (Triggered, In Analysis, Actioned, Published) |
| `note` | TEXT | Detailed notes/analysis |
| `tags` | LIST<TEXT> | Categorization tags |
| `layers` | LIST<FROZEN<cheese_layer>> | Defense layers (Nested UDT) |
| `timeline_events` | LIST<FROZEN<timeline_event>> | Incident timeline (Nested UDT) |
| `questions` | LIST<FROZEN<postmortem_question>> | Guided analysis (Nested UDT) |
| `document_uuids` | LIST<UUID> | References to attached documents |
| `local_postmortem_uuids` | LIST<UUID> | Linked local postmortems (for Major) |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

### 2. `postmortem_document`
Stores metadata and content for attached files.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `postmortem_uuid` | UUID | Reference to parent postmortem |
| `filename` | TEXT | Name of the file |
| `content_type` | TEXT | MIME type |
| `data` | BLOB | Binary file content |
| `upload_time` | TIMESTAMP | When it was uploaded |

### 3. `users`
System user accounts.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `username` | TEXT | Unique username |
| `password` | TEXT | Encrypted password |
| `email` | TEXT | User email |
| `roles` | SET<TEXT> | Assigned roles (e.g., ROLE_ADMIN) |
| `allowed_departments` | SET<TEXT> | Accessible departments |
| `active` | BOOLEAN | Account status |

---

## User-Defined Types (UDTs)

These types are used to structure data within the `postmortem` table.

### `cheese_layer`
Represents a layer of defense in the Swiss Cheese Model.
- `uuid`: UUID
- `name`: TEXT (e.g., "Design", "Test")
- `description`: TEXT
- `holes`: LIST<FROZEN<hole>> (Nested UDT)

### `hole`
Represents a weakness identified within a layer.
- `uuid`: UUID
- `description`: TEXT
- `team_name`: TEXT
- `remedial_action`: TEXT
- `action_status`: TEXT
- `story`: FROZEN<story> (Optional nested UDT)

### `story`
Represents a remedial action task.
- `uuid`: UUID
- `story_number`: TEXT (External ID)
- `team_name`: TEXT
- `backlog_name`: TEXT
- `platform`: TEXT
- `what_to_fix`: TEXT
- `priority`: TEXT
- `status`: TEXT
- `story_link`: TEXT

### `timeline_event`
- `uuid`: UUID
- `event_time`: TIMESTAMP
- `description`: TEXT

### `postmortem_question`
- `uuid`: UUID
- `cheese_layer`: TEXT
- `question`: TEXT
- `answer`: TEXT

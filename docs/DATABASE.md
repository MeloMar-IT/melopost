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
| `service_impacts` | LIST<FROZEN<service_impact>> | Service impacts (Nested UDT) |
| `cbs_impacts` | LIST<FROZEN<cbs_impact>> | CBS impacts (Nested UDT) |
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
| `file_name` | TEXT | Name of the file |
| `content_type` | TEXT | MIME type |
| `size` | BIGINT | File size in bytes |
| `data` | BLOB | Binary file content |
| `upload_date` | TIMESTAMP | When it was uploaded |

### 3. `users`
System user accounts.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `username` | TEXT | Unique username (Indexed) |
| `password` | TEXT | Encrypted password |
| `email` | TEXT | User email |
| `first_name` | TEXT | User's first name |
| `last_name` | TEXT | User's last name |
| `roles` | SET<TEXT> | Assigned roles (e.g., ROLE_ADMIN) |
| `allowed_departments` | SET<TEXT> | Accessible departments |
| `active` | BOOLEAN | Account status |

### 4. `incident_note`
External incident notes indexed by reference.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `incident_ref` | TEXT | External incident reference (Indexed) |
| `content` | TEXT | Note content |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

### 5. `data_source`
Configuration for external data sources.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `name` | TEXT | Data source name |
| `type` | TEXT | Type of source |
| `operation` | TEXT | Operation type |
| `url` | TEXT | Connection URL |
| `username` | TEXT | Login username |
| `password` | TEXT | Login password |
| `description` | TEXT | Purpose of source |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

### 6. `report_template`
Mustache templates for report generation.
| Column | Type | Description |
| :--- | :--- | :--- |
| `uuid` | UUID | Primary Key |
| `name` | TEXT | Template name |
| `content` | TEXT | Mustache template content |
| `is_default` | BOOLEAN | If this is the default template |

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
- `teamname`: TEXT
- `remedialaction`: TEXT
- `actionstatus`: TEXT
- `tags`: LIST<TEXT>
- `story`: FROZEN<story> (Optional nested UDT)

### `story`
Represents a remedial action task.
- `uuid`: UUID
- `storynumber`: TEXT (External ID)
- `teamname`: TEXT
- `backlogname`: TEXT
- `platform`: TEXT
- `whattofix`: TEXT
- `foundbydepartment`: TEXT
- `tosolvebydepartment`: TEXT
- `priority`: TEXT
- `managername`: TEXT
- `storylink`: TEXT
- `status`: TEXT
- `notes`: TEXT
- `tags`: LIST<TEXT>

### `timeline_event`
- `uuid`: UUID
- `eventtime`: TIMESTAMP
- `description`: TEXT

### `service_impact`
- `uuid`: UUID
- `service`: TEXT
- `country`: TEXT
- `start_time`: TIMESTAMP
- `end_time`: TIMESTAMP
- `duration`: TEXT
- `impact_description`: TEXT

### `cbs_impact`
- `uuid`: UUID
- `cbs_name`: TEXT
- `it_services`: TEXT
- `start_time`: TIMESTAMP
- `end_time`: TIMESTAMP
- `duration`: TEXT
- `tolerance_level_exceeded`: TEXT

### `postmortem_question`
- `uuid`: UUID
- `cheeselayer`: TEXT
- `question`: TEXT
- `answer`: TEXT

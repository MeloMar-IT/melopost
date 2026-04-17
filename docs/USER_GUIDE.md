# Melopost User Guide

Welcome to the Melopost User Guide. Melopost is a specialized application designed to manage incident postmortems and facilitate root cause analysis using the **Swiss Cheese Model**.

## Table of Contents
1. [Getting Started](#getting-started)
2. [Dashboard](#dashboard)
3. [Managing Postmortems](#managing-postmortems)
4. [Swiss Cheese Analysis](#swiss-cheese-analysis)
5. [Timeline Tracking](#timeline-tracking)
6. [Remedial Actions (Stories)](#remedial-actions)
7. [Reporting](#reporting)
8. [Data Sources](#data-sources)
9. [User Management](#user-management)

---

## Getting Started

### Login
To access Melopost, navigate to the application URL (default: `http://localhost:8080`). You will be greeted by the login screen.

![Login Screen](sceenshots/Screenshot%202026-04-09%20at%2009.50.28.png)

- **Default Credentials**: `admin` / `admin`
- Enter your username and password to continue to the Dashboard.

---

## Dashboard

The Dashboard provides a high-level overview of the incident management state. It includes:
- **Statistics**: View total postmortems, active incidents, and completion rate of remedies.
- **Filtering**: Filter postmortems by status, type, or department.
- **Recent Postmortems**: Quick access to the latest incident analyses.
- **Quick Actions**: Start a new postmortem or manage data sources directly.

---

## Managing Postmortems

### View All Postmortems
The Postmortems page lists all incident analyses in the system.

From here you can:
- **Search and Filter**: Search for specific incidents or filter by Status and Type.
- **Create**: Create a new postmortem.
- **View/Edit**: View or Edit existing postmortems.
- **State Overview**: See at a glance the Status, Type, and the number of layers/holes identified for each incident.

### Postmortem Details
Clicking on a postmortem opens its detailed view.

- **Overview**: A summary of the incident.
- **Metadata**: Details like Incident Date, Start Date, Due Date, Status, Type, and relevant departments.
- **Tags**: Categorize your incidents for better searchability.

---

## Swiss Cheese Analysis

Melopost uses 10 standard layers of defense to map incidents:
1. Define
2. Design
3. Build
4. Test
5. Release
6. Run
7. Resilience
8. Observability
9. Incident Handling
10. Human

For each layer, you can identify "Holes" (weaknesses) that contributed to the incident.

---

## Timeline Tracking

Build a chronological view of the incident by adding timeline events. This helps in understanding the sequence of failures and the response process.

---

## Remedial Actions (Stories)

Identified "holes" can be linked to actionable tasks called **Stories**.
- Track what needs to be fixed.
- Assign responsibility to departments.
- Monitor progress and status.
- Link directly to external systems like Jira, ServiceNow, or Azure DevOps.

---

## Reporting

Generate professional PDF reports for your postmortems.
- Use customizable **Mustache templates**.
- Download reports to share with stakeholders.
- Reports include the overview, timeline, Swiss cheese analysis, and remedial actions.

---

## Data Sources

Configure external system integrations in the Data Sources section.
- Support for **ServiceNow**, **Jira**, and **Azure DevOps**.
- Configure connection details and operation types (Read/Create).

---

## User Management

(Administrators Only)
- Manage user accounts and active status.
- Assign roles for access control.
- Configure department-based access restrictions.

# Melopost User Guide

Welcome to the **Melopost User Guide**. This document provides extensive and complete instructions on how to use Melopost to manage incident postmortems and perform root cause analysis using the **Swiss Cheese Model**.

---

## 📖 Table of Contents
1. [Introduction](#introduction)
2. [Key Concepts](#key-concepts)
    - [The Swiss Cheese Model](#the-swiss-cheese-model)
    - [Layers, Holes, and Stories](#layers-holes-and-stories)
3. [Getting Started](#getting-started)
    - [Login](#login)
    - [Dashboard Overview](#dashboard-overview)
4. [Managing Postmortems](#managing-postmortems)
    - [Creating a Postmortem](#creating-a-postmortem)
    - [Postmortem Types](#postmortem-types)
    - [Searching and Filtering](#searching-and-filtering)
5. [In-Depth Incident Analysis](#in-depth-incident-analysis)
    - [Timeline Management](#timeline-management)
    - [Analyzing Cheese Layers](#analyzing-cheese-layers)
    - [Identifying Holes](#identifying-holes)
    - [Answering Guiding Questions](#answering-guiding-questions)
6. [Remedial Actions (Stories)](#remedial-actions-stories)
    - [Creating Action Items](#creating-action-items)
    - [Integration with External Systems](#integration-with-external-systems)
7. [Reporting and Documentation](#reporting-and-documentation)
    - [Generating PDF Reports](#generating-pdf-reports)
    - [Uploading Documents](#uploading-documents)
    - [Adding Detailed Notes](#adding-detailed-notes)
8. [Administration](#administration)
    - [User Management](#user-management)
    - [Data Sources](#data-sources)
    - [Report Templates](#report-templates)

---

## 1. Introduction

Melopost is a specialized application designed to help organizations move beyond simple "blame games" and into structured, systematic root cause analysis. By using the Swiss Cheese Model, Melopost helps teams identify not just *who* made a mistake, but *what* layers of defense failed and *how* to prevent similar issues in the future.

## 2. Key Concepts

### The Swiss Cheese Model
In this model, an organization's defenses against failure are likened to a series of slices of Swiss cheese, stacked side by side. 
- **Slices (Layers)** represent various types of defenses (e.g., Software Design, Training, Monitoring).
- **Holes** in the slices represent individual weaknesses or failures in those defenses.
- **Incidents** occur when holes in every slice momentarily align, allowing a trajectory of accident opportunity.

### Layers, Holes, and Stories
- **Cheese Layer**: A category of defense. Melopost comes with 10 standard layers: Define, Design, Build, Test, Release, Run, Resilience, Observability, Incident Handling, and Human.
- **Hole**: A specific weakness identified within a layer during a postmortem.
- **Story**: A concrete remedial action (task) created to "plug" a hole. These can be tracked internally or linked to external systems like Jira or ServiceNow.

---

## 3. Getting Started

### Login
Access Melopost via your web browser (default: `http://localhost:8080`). 
- **Default Administrator**: `admin` / `admin`
- Ensure you change your password after the first login in the User Management section if you have admin rights.

### Dashboard Overview
The home page provides a high-level view:
- **Total Postmortems**: Quick count of all analyses performed.
- **Recent Postmortems**: A list of the latest incidents being analyzed.
- **Search**: Quickly find postmortems by title, description, or tags.

---

## 4. Managing Postmortems

### Creating a Postmortem
Click on **"Create New Postmortem"** from the dashboard or the navigation menu.
1. **Title**: A concise name for the incident.
2. **Incident Date**: When the issue actually occurred.
3. **Type**: Select from "Local postmortem", "Major Postmortem", or "Orchestrated P1".
4. **Source & Reference**: If the incident is tracked in Jira/ServiceNow, enter the ID and source.
5. **Department & Application**: Identify the owner and the affected system.

### Postmortem Types
- **Local Postmortem**: Standard analysis for a single team or service.
- **Major Postmortem**: For significant outages affecting multiple systems.
- **Orchestrated P1**: High-priority incidents that require coordination across many departments.

### Searching and Filtering
Use the search bar on the dashboard or the Postmortems list page. You can search by text or use **Tags** to filter incidents by technology, severity, or custom labels.

---

## 5. In-Depth Incident Analysis

Once a postmortem is created, click on its title to enter the **Details View**. This is where the core analysis happens.

### Timeline Management
Building a chronological sequence of events is crucial. 
- Click **"Add Event"** in the Timeline section.
- Record the exact timestamp and a brief description of what happened (e.g., "Alert triggered", "Rollback started").

### Analyzing Cheese Layers
Melopost automatically creates 10 defense layers for every postmortem.
- Review each layer to determine if it played a role in the incident.
- You can edit layer descriptions to provide context specific to the incident.

### Identifying Holes
For every layer that failed, you must identify a "Hole".
- Navigate to the specific Cheese Layer.
- Click **"Add Hole"**.
- Describe the failure, identify the team responsible for that layer, and propose a **Remedial Action**.

### Answering Guiding Questions
To help stimulate analysis, each layer may have associated questions.
- Go to the **"Questions"** tab in the Postmortem details.
- Provide answers to help document the thought process and ensure no stone is left unturned.

---

## 6. Remedial Actions (Stories)

A postmortem is only successful if it leads to improvement. "Stories" are the mechanism for this.

### Creating Action Items
For every Hole identified, you can create a Story.
- Fill in the **Story Number**, **Backlog Name**, and **Platform**.
- Define **Priority** and who is responsible for solving it.
- **Status Tracking**: Keep the status updated (e.g., Backlog, In Progress, Done) to track the progress of the fix.

### Integration with External Systems
If you have configured **Data Sources**, you can link stories to external platforms like:
- **Jira**
- **ServiceNow**
- **Azure DevOps**

---

## 7. Reporting and Documentation

### Generating PDF Reports
Once the analysis is complete, you can generate a professional report.
- Click **"Download Report"** in the Postmortem details.
- The system uses a **Report Template** (Mustache-based) to format the data into a clean PDF.

### Uploading Documents
Keep all relevant evidence in one place.
- Use the **"Documents"** tab to upload log files, architecture diagrams, or screenshots of dashboards.
- Documents are stored securely and associated directly with the postmortem.

### Adding Detailed Notes
The **"Notes"** section allows for free-form, long-form documentation. Use this for detailed technical summaries, executive summaries, or any information that doesn't fit into the structured fields.

---

## 8. Administration

Users with the **ADMIN** role have access to additional configuration:

### User Management
- Create, update, or deactivate users.
- Assign roles (`ADMIN` or `USER`).

### Data Sources
Configure connections to external systems. This allows Melopost to know where your stories and incidents are originally tracked.

### Report Templates
Customize the look and feel of your PDF reports.
- Melopost uses **Mustache** templating.
- You can create multiple templates and set one as the default.

---
*Generated by Melopost - Incident Management Simplified.*

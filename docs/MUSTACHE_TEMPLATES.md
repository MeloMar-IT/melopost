# Mustache Template Documentation

Melopost uses [Mustache](https://mustache.github.io/) templates to generate incident postmortem reports in PDF format. This document describes the data available to these templates and how you can use them to customize your reports.

## Overview

Mustache is a logic-less template engine. It uses tags like `{{variable}}` to insert data into a template. In Melopost, these templates are used to generate HTML, which is then converted into a PDF using the `OpenHTMLToPDF` library.

The default template is located at `src/main/resources/templates/reports/postmortem-report.mustache`, but you can also manage templates via the UI or API.

---

## Available Data & Sections

The following variables and sections are available when generating a postmortem report.

### Core Postmortem Information
These are top-level variables representing the basic details of the incident.

| Variable | Description | Example |
| :--- | :--- | :--- |
| `{{title}}` | The title of the postmortem. | "DB Outage in Production" |
| `{{description}}` | A detailed description of the incident. | "The primary database..." |
| `{{type}}` | The category or type of the postmortem. | "Infrastructure" |
| `{{incidentRef}}` | A reference ID for the incident. | "INC-12345" |
| `{{incidentSource}}` | The source of the incident reference. | "ServiceNow" |
| `{{incidentDate}}` | The date and time the incident occurred. | "Oct 27, 2023 14:30" |
| `{{startDate}}` | When the analysis started. | "Oct 28, 2023 09:00" |
| `{{postMortemMeetingDate}}` | When the postmortem meeting took place. | "Oct 30, 2023 10:00" |
| `{{dueDate}}` | The deadline for completing the analysis. | "Nov 15, 2023" |
| `{{storyStore}}` | Where remedial actions are tracked. | "Jira" |
| `{{department}}` | The department responsible or affected. | "Platform Engineering" |
| `{{failedApplication}}` | The application that failed during the incident. | "Payment Gateway" |

### Tags
Tags are a list of strings associated with the postmortem.

```mustache
{{#tags}}
    <span class="tag">{{.}}</span>
{{/tags}}
{{^tags}}
    <p>No tags assigned.</p>
{{/tags}}
```

### Timeline Sections
Melopost provides multiple ways to display the incident timeline.

#### 1. Chronological Event List (`timelineEvents`)
A simple list of all events in the timeline.

| Variable | Description |
| :--- | :--- |
| `{{eventTime}}` | The formatted time of the event. |
| `{{description}}` | What happened at this time. |

#### 2. Graphical Timeline (`graphicalTimeline`)
A structured list optimized for a visual vertical timeline.

| Variable | Description |
| :--- | :--- |
| `{{date}}` | Date formatted as "MMM dd" (e.g., Oct 27). |
| `{{time}}` | Time formatted as "HH:mm" (e.g., 14:30). |
| `{{description}}` | Event description. |

*Use `{{#hasGraphicalTimeline}}...{{/hasGraphicalTimeline}}` to conditionally show this section.*

#### 3. Mermaid/Kroki Integration (`mermaidSource`)
If timeline events exist, Melopost generates a [Mermaid.js](https://mermaid.js.org/) timeline diagram. This diagram is encoded and can be displayed using [Kroki](https://kroki.io/).

```mustache
{{#mermaidSource}}
    <img src="https://kroki.io/mermaid/png/{{mermaidSource}}" alt="Incident Timeline" />
{{/mermaidSource}}
```

---

### Swiss Cheese Analysis (`layers`)
This is the core of the postmortem analysis, mapping the incident to defensive layers.

```mustache
{{#layers}}
    <h3>{{name}}</h3>
    <p>{{description}}</p>
    
    {{#holes}}
        <div class="hole">
            <strong>Hole:</strong> {{description}}<br/>
            <strong>Team:</strong> {{teamName}}<br/>
            <strong>Remedial Action:</strong> {{remedialAction}} ({{actionStatus}})
            
            {{#tags}}
                <span class="tag">{{.}}</span>
            {{/tags}}
            
            {{#story}}
                <br/><strong>Story:</strong> {{storyNumber}} - {{status}}
                {{#priority}}<br/><strong>Priority:</strong> {{priority}}{{/priority}}
                {{#whatToFix}}<br/><strong>What to fix:</strong> {{whatToFix}}{{/whatToFix}}
                {{#storyLink}}<br/><strong>Link:</strong> {{storyLink}}{{/storyLink}}
            {{/story}}
            
            {{#sourcePostmortem}}
                <br/><small>Source: {{sourcePostmortem}}</small>
            {{/sourcePostmortem}}
        </div>
    {{/holes}}
{{/layers}}
```

#### Layer Variables:
- `{{name}}`: Name of the defensive layer (e.g., "Monitoring").
- `{{description}}`: Description of the layer.
- `{{holes}}`: List of vulnerabilities identified in this layer.

#### Hole Variables:
- `{{description}}`: What went wrong at this layer.
- `{{teamName}}`: The team responsible for this hole.
- `{{remedialAction}}`: The action plan to fix it.
- `{{actionStatus}}`: Current status of the remedial action.
- `{{tags}}`: List of tags for this hole.
- `{{sourcePostmortem}}`: If the hole was pulled from a linked local postmortem, this shows the source.
- `{{story}}`: An optional object containing tracking details for the remedial action.

#### Story Variables:
- `{{storyNumber}}`: The ID of the story in the tracking system (e.g., Jira-123).
- `{{status}}`: Current status of the story.
- `{{priority}}`: Priority level.
- `{{whatToFix}}`: Specific technical details on the fix.
- `{{storyLink}}`: Direct URL to the story.

---

### Analysis Questions (`questions`)
A list of pre-defined or custom questions answered during the analysis.

| Variable | Description |
| :--- | :--- |
| `{{cheeseLayer}}` | The layer this question refers to. |
| `{{question}}` | The text of the question. |
| `{{answer}}` | The answer provided. |

---

### Linked Postmortems
Melopost allows linking related postmortems.

#### Local Postmortems (`localPostmortems`)
Linked postmortems from the same system. Note that holes from these postmortems are **automatically aggregated** into the main `layers` section, but you can also list the sources separately.

- Variables: `{{id}}`, `{{title}}`, `{{incidentRef}}`, `{{incidentDate}}`, and even nested `{{layers}}`.

#### Major Postmortems (`majorPostmortems`)
References to parent or broader incident analyses.

- Variables: `{{id}}`, `{{title}}`, `{{incidentRef}}`, `{{incidentDate}}`.

---

## Best Practices
1. **Conditional Sections**: Use `{{#section}}...{{/section}}` to hide empty parts of the report.
2. **Date Formatting**: Melopost provides pre-formatted dates. Use them as-is for consistency.
3. **Escaping**: Mustache escapes HTML by default. Use `{{{variable}}}` (triple braces) if you need to render raw HTML from the database (use with caution).
4. **Styling**: Include a `<style>` block in your template to control font, colors, and page breaks for the PDF.

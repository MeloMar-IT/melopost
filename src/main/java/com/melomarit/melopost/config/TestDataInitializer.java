package com.melomarit.melopost.config;

import com.melomarit.melopost.model.*;
import com.melomarit.melopost.repository.DataSourceRepository;
import com.melomarit.melopost.repository.IncidentNoteRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.ReportTemplateRepository;
import com.melomarit.melopost.repository.UserRepository;
import com.melomarit.melopost.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Configuration
public class TestDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);
    private final Random random = new Random();

    private final String[] layerNames = {"Define", "Design", "Build", "Test", "Release", "Run", "Resilience", "Observability", "Incident Handling", "Human Factors"};
    private final String[] teamNames = {"SRE Team", "Platform Engineering", "Core Infrastructure", "Security Operations", "Database Reliability", "Network Engineering", "Frontend Core", "Payments Services", "API Gateway Team"};
    private final String[] statuses = {"PENDING", "COMPLETED", "IN_PROGRESS", "BLOCKED", "DEFERRED"};
    private final String[] priorities = {"LOW", "MEDIUM", "HIGH", "CRITICAL", "EMERGENCY"};
    private final String[] departments = {"Platform Operations", "Cloud Infrastructure", "Cyber Security", "Engineering Services", "Product Delivery", "Customer Support", "Data Science"};
    private final String[] failedApplications = {
        "API Gateway (Kong/Istio)", "Global Checkout Service", "PostgreSQL Cluster (Primary)", 
        "Redis Cache Layer", "Kafka Message Bus", "Elasticsearch Cluster", "Identity Service (OAuth2)", 
        "Notification Engine", "Billing Middleware", "Prometheus/Grafana Stack", "Kubernetes Ingress Controller"
    };
    private final String[] tags = {
        "outage", "latency", "data-loss", "security-vulnerability", "infra-failure", "network-partition", 
        "resource-exhaustion", "concurrency-issue", "dependency-failure", "misconfiguration"
    };
    private final String[] incidentTitles = {
        "Cascading failure in Microservices due to Timeout settings",
        "Database Deadlock in Payment Processing under high load",
        "DNS Resolution issues affecting Internal Services",
        "Memory Leak in Auth Service following v2.4 deployment",
        "Unexpected Cloud Provider Region Outage (us-east-1)",
        "API Rate Limiting causing 429s for Mobile Clients",
        "Stale Cache causing incorrect pricing on Checkout page",
        "Zombie processes on Worker Nodes leading to CPU exhaustion",
        "Security: Unauthorized access attempt via SQL Injection",
        "Kubernetes OOMKilled events on Edge Proxy pods",
        "SSL Certificate expiration on non-production API",
        "Data discrepancy in Warehouse due to failed ETL job",
        "Slow SQL queries causing thread pool saturation",
        "Disk space exhaustion on Log Aggregator nodes",
        "Misconfigured Load Balancer routing traffic to unhealthy nodes"
    };
    private final String[] incidentDescriptions = {
        "The incident started when a sudden spike in traffic caused the primary database to reach its connection limit.",
        "A routine deployment of the billing service introduced a regression that caused double-billing for some users.",
        "Internal DNS records were inadvertently deleted during a Terraform apply, causing widespread service disruption.",
        "An unoptimized SQL query was introduced, leading to high CPU usage and eventual database lock contention.",
        "The authentication service started failing with 5xx errors due to a dependency failure in the underlying vault provider.",
        "We observed a significant increase in latency for the search API, which was traced back to an inefficient index.",
        "A misconfigured firewall rule blocked traffic between the web servers and the application backend.",
        "Memory exhaustion on the Redis cluster led to massive eviction and subsequent performance degradation.",
        "The automated backup process failed silently for 3 days due to a change in S3 bucket permissions.",
        "A rare race condition in the message consumer caused messages to be processed out of order."
    };
    private final String[] rootCauses = {
        "Inadequate timeout and retry logic in the client library.",
        "Missing database index on the 'order_id' column.",
        "Manual configuration change bypassed the CI/CD pipeline.",
        "Hidden dependency on a legacy service that was being decommissioned.",
        "Insufficient monitoring and alerting on disk space usage.",
        "Upstream service outage caused by a major cloud provider failure.",
        "Default configuration settings were not optimized for high throughput.",
        "Code regression introduced in the last sprint's security patch.",
        "Hardware failure in the underlying virtualization host.",
        "Unexpected volume of traffic from a newly launched marketing campaign."
    };
    private final String[] remedialActions = {
        "Implement circuit breaker pattern for all external service calls.",
        "Migrate to a more resilient database architecture with multi-region failover.",
        "Automate the deployment process to eliminate manual configuration errors.",
        "Increase monitoring coverage to include granular metrics for thread pools.",
        "Refactor the authentication logic to handle upstream failures gracefully.",
        "Update the CI/CD pipeline to include mandatory performance testing.",
        "Optimize the SQL queries identified during the incident analysis.",
        "Review and update the disaster recovery plan for the core infrastructure.",
        "Implement better rate limiting and request throttling.",
        "Upgrade the Redis cluster to the latest stable version with improved clustering."
    };
    private final String[] questionTexts = {
        "What was the first sign of trouble in the observability stack?",
        "Did we have automated alerts that triggered for this incident?",
        "Could this failure mode have been detected by existing health checks?",
        "Who was the incident commander and what was the communication channel?",
        "Was there a recent change in the infrastructure that preceded this?",
        "How long did it take from detection to first remediation attempt?",
        "Is there a permanent fix scheduled in the current sprint backlog?",
        "How many users were impacted by this service degradation?",
        "What was the Mean Time to Recovery (MTTR) for this incident?"
    };

    private final String[] services = {
        "User Authentication", "Payment Gateway", "Order Management", "Inventory Sync", 
        "Product Search", "Shopping Cart", "Email Notifications", "Analytics Dashboard"
    };

    private final String[] countries = {
        "Global", "United Kingdom", "Germany", "France", "Netherlands", "United States", "Japan", "Brazil"
    };

    private final String[] cbsNames = {
        "CBS - Core Banking System", "CBS - Wealth Management", "CBS - Retail Banking", 
        "CBS - Corporate Loans", "CBS - Payments HUB"
    };

    private final String[] incidentNoteContents = {
        "#### Timeline Observation\nInvestigated the logs from `api-gateway-748f9d6b5c-2x9v4`. Found multiple `java.net.SocketTimeoutException` occurring around 14:05 UTC. This matches the start of the latency spike reported by monitoring.",
        "#### Investigation Update\nChecked the database connection pool metrics. The `hikari-pool-1` was exhausted (100/100 connections in use). Most connections were in `ACTIVE` state, executing a slow `JOIN` query on the `orders` and `transactions` tables.",
        "#### Root Cause Analysis\nThe recent deployment (v2.4.1) introduced a regression in the `PaymentService.process()` method where a database transaction was held open while making an external API call to the credit card processor.",
        "#### Mitigation Strategy\nTemporary workaround: Increased the maximum connection pool size from 100 to 250 and reduced the socket timeout to 2 seconds. This should provide some headroom until a proper fix is deployed.",
        "#### Communication Log\nSynchronized with the Frontend team. They have implemented a retry-with-exponential-backoff strategy in the mobile app to reduce the load on the gateway during 503/504 errors.",
        "#### Security Review\nNo signs of unauthorized access or data exfiltration. The high traffic spike was legitimate organic traffic combined with a misconfigured health check from the internal monitoring system.",
        "#### Infrastructure Note\nThe underlying Kubernetes nodes were under heavy pressure. Observed `disk pressure` on `node-03`. Cleared `/var/log` to free up space and moved the log aggregation agent to a separate volume.",
        "#### Performance Insight\nProfiling the `ReportGenerator` service showed that it spends 40% of its time in G1GC. Increased the heap size to 8GB and adjusted the `-XX:MaxGCPauseMillis` parameter."
    };

    private final String[] postmortemNoteContents = {
        "### Summary of Findings\n\nThe incident was primarily caused by a **cascading failure** initiated by a slow database query. The lack of a circuit breaker in the `OrderService` meant that the failure propagated upstream to the `CheckoutService`, eventually exhausting all available worker threads.\n\n### Key Lessons Learned\n1. Always use timeouts for database queries.\n2. Implement circuit breakers for all synchronous service-to-service calls.\n3. Improve the granularity of alerts for connection pool exhaustion.\n\n### Next Steps\n- [ ] Deploy v2.4.2 with the fixed SQL query.\n- [ ] Configure Resilience4j on the `OrderService` client.\n- [ ] Update the Grafana dashboard to include thread pool utilization per service.",
        "### Detailed Analysis\n\nThe root cause was a **race condition** in the distributed locking mechanism used for inventory updates. When two concurrent requests attempted to lock the same SKU, a deadlock occurred in the Redis Lua script under high contention.\n\n### Technical Breakdown\n- **Service**: InventoryManager\n- **Component**: RedisDistributedLock\n- **Version**: v1.2.0\n\n### Remediation\nWe have decided to move away from Lua-based locking and implement a more robust optimistic concurrency control at the database level for SKU updates. This will be tracked in JIRA-44921.",
        "### Incident Retrospective\n\nThis outage highlighted a significant gap in our **disaster recovery** documentation. While the automated failover to the secondary region worked, the manual DNS update took longer than expected due to missing permissions for the on-call engineer.\n\n### Improvements\n* Automated the DNS failover using AWS Route53 health checks.\n* Updated the IAM roles for the SRE on-call rotation.\n* Conducted a mock-failover exercise with the team.",
        "### Observations on Observability\n\nDuring the incident, the Prometheus server was unable to scrape metrics from the affected pods because the CPU was pegged at 100%. This led to a 'blind spot' for about 15 minutes.\n\n### Recommendations\n1. Implement **sidecar metrics exporting** with dedicated resource limits.\n2. Set up alerts based on external blackbox monitoring (e.g., Synthetic tests).\n3. Increase the CPU reservation for the core infrastructure pods."
    };

    @Bean
    @Order(3)
    public CommandLineRunner initTestData(
            PostmortemRepository postmortemRepository,
            DataSourceRepository dataSourceRepository,
            DataSourceService dataSourceService,
            IncidentNoteRepository incidentNoteRepository,
            UserRepository userRepository,
            ReportTemplateRepository reportTemplateRepository,
            ResourceLoader resourceLoader,
            PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("[TEST_DATA] Removing existing test data...");
            
            postmortemRepository.deleteAll();
            dataSourceRepository.deleteAll();
            incidentNoteRepository.deleteAll();

            // Initialize User
            if (userRepository.count() <= 1) { // If only admin or no users exist
                log.info("[TEST_DATA] Fewer than 2 users found, checking/initializing users...");
                if (userRepository.findByUsername("admin").isEmpty()) {
                    User admin = new User();
                    admin.setUuid(UUID.randomUUID());
                    admin.setUsername("admin");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setEmail("admin@example.com");
                    admin.setRoles(Set.of("ADMIN", "USER"));
                    admin.setActive(true);
                    userRepository.save(admin);
                    log.info("[TEST_DATA] Initialized default admin user.");
                }

                if (userRepository.findByUsername("user1").isEmpty()) {
                    User user1 = new User();
                    user1.setUuid(UUID.randomUUID());
                    user1.setUsername("user1");
                    user1.setPassword(passwordEncoder.encode("user1"));
                    user1.setEmail("user1@example.com");
                    user1.setRoles(Set.of("USER"));
                    user1.setActive(true);
                    userRepository.save(user1);
                }

                if (userRepository.findByUsername("user2").isEmpty()) {
                    User user2 = new User();
                    user2.setUuid(UUID.randomUUID());
                    user2.setUsername("user2");
                    user2.setPassword(passwordEncoder.encode("user2"));
                    user2.setEmail("user2@example.com");
                    user2.setRoles(Set.of("USER"));
                    user2.setActive(true);
                    userRepository.save(user2);
                }
                log.info("[TEST_DATA] Initialized test users (user1, user2).");
            }

            // Initialize Default Report Template
            if (reportTemplateRepository.count() >= 0) { // Changed to always check/update to ensure latest template
                try {
                    // Default Template
                    String templateName = "Standard Postmortem Report";
                    var existingTemplate = reportTemplateRepository.findByName(templateName);

                    var resource = resourceLoader.getResource("classpath:templates/reports/postmortem-report.mustache");
                    String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                    if (existingTemplate.isPresent()) {
                        ReportTemplate t = existingTemplate.get();
                        if (!content.equals(t.getContent())) {
                            t.setContent(content);
                            reportTemplateRepository.save(t);
                            log.info("[TEST_DATA] Updated existing default report template with latest content.");
                        }
                    } else {
                        ReportTemplate template = new ReportTemplate();
                        template.setUuid(UUID.randomUUID());
                        template.setName(templateName);
                        template.setContent(content);
                        template.setDefault(true);
                        reportTemplateRepository.save(template);
                        log.info("[TEST_DATA] Initialized default report template.");
                    }

                    // Test Template
                    String testTemplateName = "Test Mustache Template";
                    var existingTestTemplate = reportTemplateRepository.findByName(testTemplateName);

                    var testResource = resourceLoader.getResource("classpath:templates/reports/test.mustache");
                    String testContent = new String(testResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                    if (existingTestTemplate.isPresent()) {
                        ReportTemplate t = existingTestTemplate.get();
                        if (!testContent.equals(t.getContent())) {
                            t.setContent(testContent);
                            reportTemplateRepository.save(t);
                            log.info("[TEST_DATA] Updated existing test report template with latest content.");
                        }
                    } else {
                        ReportTemplate testTemplate = new ReportTemplate();
                        testTemplate.setUuid(UUID.randomUUID());
                        testTemplate.setName(testTemplateName);
                        testTemplate.setContent(testContent);
                        testTemplate.setDefault(false);
                        reportTemplateRepository.save(testTemplate);
                        log.info("[TEST_DATA] Initialized test report template.");
                    }
                } catch (Exception e) {
                    log.error("[TEST_DATA] Error initializing/updating report templates: {}", e.getMessage());
                }
            }

            log.info("[TEST_DATA] Initializing test data...");
            List<DataSource> templates = dataSourceService.getTemplates();
            
            for (DataSource template : templates) {
                // 1. Ensure DataSource exists in DB (even if they are just templates)
                DataSource ds = new DataSource();
                ds.setUuid(UUID.randomUUID());
                ds.setName(template.getName());
                ds.setType(template.getType());
                ds.setOperation(template.getOperation());
                ds.setUrl(template.getUrl());
                ds.setDescription(template.getDescription());
                dataSourceRepository.save(ds);

                log.info("[TEST_DATA] Creating 5 postmortems for template: {}", template.getName());
                
                for (int i = 1; i <= 5; i++) {
                    Postmortem pm = createRandomPostmortem(template, i);
                    postmortemRepository.save(pm);

                    // Add 1-3 random incident notes for each postmortem
                    int notesCount = 1 + random.nextInt(3);
                    for (int n = 0; n < notesCount; n++) {
                        IncidentNote note = new IncidentNote();
                        note.onCreate();
                        note.setIncidentRef(pm.getIncidentRef());
                        note.setContent(incidentNoteContents[random.nextInt(incidentNoteContents.length)]);
                        incidentNoteRepository.save(note);
                    }
                }
            }
            log.info("[TEST_DATA] Data initialization complete.");
        };
    }

    private Postmortem createRandomPostmortem(DataSource template, int index) {
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());
        pm.setTitle(incidentTitles[random.nextInt(incidentTitles.length)] + " [" + template.getType() + "]");
        pm.setDescription(incidentDescriptions[random.nextInt(incidentDescriptions.length)] + 
                "\n\nContext: This incident affected the " + template.getName() + " environment and was discovered during peak hours.");
        pm.setIncidentDate(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)));
        pm.setStartDate(pm.getIncidentDate().minusHours(1 + random.nextInt(12)));
        pm.setPostMortemMeetingDate(pm.getIncidentDate().plusDays(2 + random.nextInt(7)));
        pm.setIncidentRef(template.getType().substring(0, 1).toUpperCase() + "-" + (1000 + random.nextInt(9000)));
        pm.setIncidentSource(template.getName());
        pm.setStoryStore(template.getName()); 
        pm.setDepartment(departments[random.nextInt(departments.length)]);
        pm.setFailedApplication(failedApplications[random.nextInt(failedApplications.length)]);
        pm.setType(Postmortem.POSTMORTEM_TYPES.get(random.nextInt(Postmortem.POSTMORTEM_TYPES.size())));
        pm.setNote(postmortemNoteContents[random.nextInt(postmortemNoteContents.length)]);

        // Add 2-5 random tags
        int pmTagCount = 2 + random.nextInt(4);
        for (int t = 0; t < pmTagCount; t++) {
            pm.getTags().add(tags[random.nextInt(tags.length)]);
        }

        // Always add all standard layers
        for (String layerName : layerNames) {
            CheeseLayerUDT layer = new CheeseLayerUDT();
            layer.setUuid(UUID.randomUUID());
            layer.setName(layerName);
            layer.setDescription("Detailed analysis of the '" + layer.getName() + "' aspects related to the incident.");

            // Add 0-3 random holes per layer
            int holeCount = random.nextInt(4); 
            for (int k = 0; k < holeCount; k++) {
                HoleUDT hole = new HoleUDT();
                hole.setUuid(UUID.randomUUID());
                String rc = rootCauses[random.nextInt(rootCauses.length)];
                hole.setDescription("Vulnerability in " + layer.getName() + " layer: " + rc);
                hole.setTeamName(teamNames[random.nextInt(teamNames.length)]);
                hole.setRemedialAction(remedialActions[random.nextInt(remedialActions.length)]);
                hole.setActionStatus(statuses[random.nextInt(statuses.length)]);

                // Add 1-3 random hole tags
                int holeTagCount = 1 + random.nextInt(3);
                for (int t = 0; t < holeTagCount; t++) {
                    hole.getTags().add(tags[random.nextInt(tags.length)]);
                }

                StoryUDT story = new StoryUDT();
                story.setUuid(UUID.randomUUID());
                story.setStoryNumber("TICKET-" + (10000 + random.nextInt(90000)));
                story.setTeamName(hole.getTeamName());
                story.setBacklogName(story.getTeamName() + " Backlog");
                story.setPlatform(template.getType());
                story.setWhatToFix("Resolve: " + rc);
                story.setFoundByDepartment(departments[random.nextInt(departments.length)]);
                story.setToSolveByDepartment(departments[random.nextInt(departments.length)]);
                story.setPriority(priorities[random.nextInt(priorities.length)]);
                story.setManagerName("Lead " + (char)('A' + random.nextInt(26)) + ". Developer");
                story.setStoryLink(template.getUrl() + "/" + story.getStoryNumber());
                story.setStatus(hole.getActionStatus().equals("COMPLETED") ? "CLOSED" : "OPEN");
                story.setNotes("Root cause analysis indicates this was a recurring bottleneck.");

                hole.setStory(story);
                layer.getHoles().add(hole);
            }
            pm.getLayers().add(layer);
        }

        // Add 4-8 random timeline events
        int eventCount = 4 + random.nextInt(5);
        String[] timelineEventsStrings = {
            "Initial alert triggered in PagerDuty.",
            "Incident commander assigned and Slack channel #incident-response created.",
            "Engineers began investigating the database logs.",
            "Identified a spike in 5xx errors from the Edge Proxy.",
            "First attempt at remediation: restarting the affected service pods.",
            "Confirmed that the service restart did not resolve the issue.",
            "Rolled back the most recent deployment of the Payments service.",
            "Service health restored. Monitoring for stability.",
            "Incident resolved. Postmortem scheduled."
        };
        for (int m = 0; m < eventCount && m < timelineEventsStrings.length; m++) {
            TimelineEventUDT event = new TimelineEventUDT();
            event.setUuid(UUID.randomUUID());
            event.setEventTime(pm.getIncidentDate().plusMinutes(m * 20));
            event.setDescription(timelineEventsStrings[m]);
            pm.getTimelineEvents().add(event);
        }
        
        // Add 3-6 random questions
        int qCount = 3 + random.nextInt(4);
        for (int q = 0; q < qCount; q++) {
            PostmortemQuestionUDT question = new PostmortemQuestionUDT();
            question.setUuid(UUID.randomUUID());
            question.setQuestion(questionTexts[random.nextInt(questionTexts.length)]);
            if (random.nextBoolean()) {
                question.setAnswer("Our investigation showed that " + incidentDescriptions[random.nextInt(incidentDescriptions.length)].toLowerCase());
            }
            if (!pm.getLayers().isEmpty()) {
                question.setCheeseLayer(pm.getLayers().get(random.nextInt(pm.getLayers().size())).getName());
            } else {
                question.setCheeseLayer("General");
            }
            pm.getQuestions().add(question);
        }

        // Add 1-3 Service Impacts
        int serviceImpactCount = 1 + random.nextInt(3);
        for (int i = 0; i < serviceImpactCount; i++) {
            ServiceImpactUDT si = new ServiceImpactUDT();
            si.setUuid(UUID.randomUUID());
            si.setService(services[random.nextInt(services.length)]);
            si.setCountry(countries[random.nextInt(countries.length)]);
            si.setStartTime(pm.getStartDate().plusMinutes(random.nextInt(60)));
            si.setEndTime(si.getStartTime().plusMinutes(30 + random.nextInt(120)));
            si.setDuration("1h 30m"); // Simplified
            si.setImpactDescription("Degraded performance and intermittent timeouts for users in " + si.getCountry());
            pm.getServiceImpacts().add(si);
        }

        // Add 1-2 CBS Impacts
        int cbsImpactCount = 1 + random.nextInt(2);
        for (int i = 0; i < cbsImpactCount; i++) {
            CbsImpactUDT ci = new CbsImpactUDT();
            ci.setUuid(UUID.randomUUID());
            ci.setCbsName(cbsNames[random.nextInt(cbsNames.length)]);
            ci.setItServices("Trading, Settlements, Reporting");
            ci.setStartTime(pm.getStartDate().plusMinutes(random.nextInt(60)));
            ci.setEndTime(ci.getStartTime().plusMinutes(60 + random.nextInt(180)));
            ci.setDuration("2h 15m"); // Simplified
            ci.setToleranceLevelExceeded(random.nextBoolean() ? "Yes" : "No");
            pm.getCbsImpacts().add(ci);
        }

        return pm;
    }
}

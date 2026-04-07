package com.melo.melopost.config;

import com.melo.melopost.model.*;
import com.melo.melopost.repository.DataSourceRepository;
import com.melo.melopost.repository.PostmortemRepository;
import com.melo.melopost.repository.ReportTemplateRepository;
import com.melo.melopost.repository.UserRepository;
import com.melo.melopost.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
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

@Configuration
public class TestDataInitializer {
    private static final Logger log = LoggerFactory.getLogger(TestDataInitializer.class);
    private final Random random = new Random();

    private final String[] layerNames = {"Define", "Design", "Build", "Test", "Release", "Run", "Resilience", "Observability", "Incident Handling", "Human"};
    private final String[] teamNames = {"DevOps", "Frontend", "Backend", "Security", "SRE", "Infrastructure"};
    private final String[] statuses = {"PENDING", "COMPLETED", "IN_PROGRESS"};
    private final String[] priorities = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
    private final String[] departments = {"IT", "Operations", "QA", "Development", "Security"};
    private final String[] tags = {"performance", "database", "ui", "api", "security", "infra", "process", "human-error"};
    private final String[] questionTexts = {
        "What was the first sign of trouble?",
        "Did we have monitoring for this?",
        "Could this have been prevented?",
        "Who was involved in the initial response?",
        "Was there a recent change that could have caused this?",
        "How long did it take to identify the root cause?",
        "Is there a permanent fix planned?"
    };

    @Bean
    public CommandLineRunner initTestData(
            PostmortemRepository postmortemRepository,
            DataSourceRepository dataSourceRepository,
            DataSourceService dataSourceService,
            UserRepository userRepository,
            ReportTemplateRepository reportTemplateRepository,
            ResourceLoader resourceLoader,
            PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("[TEST_DATA] Checking for existing data...");
            
            // Initialize User
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setEmail("admin@example.com");
                admin.setRoles(Set.of("ADMIN", "USER"));
                admin.setActive(true);
                userRepository.save(admin);
                log.info("[TEST_DATA] Initialized default admin user.");
            }

            // Initialize Default Report Template
            if (reportTemplateRepository.count() == 0) {
                try {
                    // Default Template
                    ReportTemplate template = new ReportTemplate();
                    template.setName("Standard Postmortem Report");
                    var resource = resourceLoader.getResource("classpath:templates/reports/postmortem-report.mustache");
                    String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    template.setContent(content);
                    template.setDefault(true);
                    reportTemplateRepository.save(template);
                    log.info("[TEST_DATA] Initialized default report template.");

                    // Test Template
                    ReportTemplate testTemplate = new ReportTemplate();
                    testTemplate.setName("Test Mustache Template");
                    var testResource = resourceLoader.getResource("classpath:templates/reports/test.mustache");
                    String testContent = new String(testResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    testTemplate.setContent(testContent);
                    testTemplate.setDefault(false);
                    reportTemplateRepository.save(testTemplate);
                    log.info("[TEST_DATA] Initialized test report template.");
                } catch (Exception e) {
                    log.error("[TEST_DATA] Error initializing report templates: {}", e.getMessage());
                }
            }

            if (postmortemRepository.count() > 0) {
                log.info("[TEST_DATA] Database already contains postmortems. Skipping initialization.");
                return;
            }

            log.info("[TEST_DATA] Initializing test data...");
            List<DataSource> templates = dataSourceService.getTemplates();
            
            for (DataSource template : templates) {
                // 1. Ensure DataSource exists in DB (even if they are just templates)
                DataSource ds = new DataSource();
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
                }
            }
            log.info("[TEST_DATA] Data initialization complete.");
        };
    }

    private Postmortem createRandomPostmortem(DataSource template, int index) {
        Postmortem pm = new Postmortem();
        pm.setTitle(template.getType() + " Incident #" + (index + random.nextInt(1000)));
        pm.setDescription("This is a random test postmortem for " + template.getName() + ". Detailed analysis of what went wrong.");
        pm.setIncidentDate(LocalDateTime.now().minusDays(random.nextInt(30)));
        pm.setStartDate(pm.getIncidentDate().minusHours(random.nextInt(12)));
        pm.setPostMortemMeetingDate(pm.getIncidentDate().plusDays(3 + random.nextInt(10)));
        pm.setIncidentRef(template.getType().substring(0, 1).toUpperCase() + "-" + (1000 + random.nextInt(9000)));
        pm.setIncidentSource(template.getType());
        pm.setStoryApplication(template.getType()); // Using the same platform for stories as incident source

        // Add 1-3 random tags
        int pmTagCount = 1 + random.nextInt(3);
        for (int t = 0; t < pmTagCount; t++) {
            pm.getTags().add(tags[random.nextInt(tags.length)]);
        }

        // Always add all 10 standard layers to guide the analysis as per documentation
        for (String layerName : layerNames) {
            CheeseLayer layer = new CheeseLayer();
            layer.setName(layerName);
            layer.setDescription("Analysis of " + layer.getName() + " layer.");

            // Add 0-2 random holes per layer
            int holeCount = random.nextInt(3); 
            for (int k = 0; k < holeCount; k++) {
                Hole hole = new Hole();
                hole.setDescription("Hole " + (k + 1) + " in " + layer.getName() + ": Root cause discovered.");
                hole.setTeamName(teamNames[random.nextInt(teamNames.length)]);
                hole.setRemedialAction("Implement fix for hole " + (k + 1));
                hole.setActionStatus(statuses[random.nextInt(statuses.length)]);

                // Add 1-2 random hole tags
                int holeTagCount = 1 + random.nextInt(2);
                for (int t = 0; t < holeTagCount; t++) {
                    hole.getTags().add(tags[random.nextInt(tags.length)]);
                }

                Story story = new Story();
                story.setStoryNumber("STORY-" + (10000 + random.nextInt(90000)));
                story.setTeamName(hole.getTeamName());
                story.setBacklogName(story.getTeamName() + " Backlog");
                story.setPlatform(template.getType());
                story.setWhatToFix("Fix the issue in " + layer.getName());
                story.setFoundByDepartment(departments[random.nextInt(departments.length)]);
                story.setToSolveByDepartment(departments[random.nextInt(departments.length)]);
                story.setPriority(priorities[random.nextInt(priorities.length)]);
                story.setManagerName("Manager " + (char)('A' + random.nextInt(26)));
                story.setStoryLink(template.getUrl() + "/" + story.getStoryNumber());
                story.setStatus("OPEN");
                story.setNotes("Test note for story " + story.getStoryNumber());

                hole.setStory(story);
                layer.getHoles().add(hole);
            }
            pm.addLayer(layer);
        }

        // Add 3-5 random timeline events
        int eventCount = 3 + random.nextInt(3);
        for (int m = 0; m < eventCount; m++) {
            TimelineEvent event = new TimelineEvent();
            event.setEventTime(pm.getIncidentDate().plusMinutes(m * 15));
            event.setDescription("Event " + (m + 1) + ": Progress during incident handling.");
            pm.getTimelineEvents().add(event);
        }
        
        // Add 2-4 random questions
        int qCount = 2 + random.nextInt(3);
        for (int q = 0; q < qCount; q++) {
            PostmortemQuestion question = new PostmortemQuestion();
            question.setQuestion(questionTexts[random.nextInt(questionTexts.length)]);
            if (random.nextBoolean()) {
                question.setAnswer("Automated test data answer for question " + (q + 1));
            }
            if (!pm.getLayers().isEmpty()) {
                question.setCheeseLayer(pm.getLayers().get(random.nextInt(pm.getLayers().size())).getName());
            } else {
                question.setCheeseLayer("General");
            }
            question.setPostmortem(pm);
            pm.getQuestions().add(question);
        }

        return pm;
    }
}

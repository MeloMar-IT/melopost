package com.melomarit.melopost.service;

import com.melomarit.melopost.dto.HoleDTO;
import com.melomarit.melopost.dto.PostmortemSearchResultDTO;
import com.melomarit.melopost.dto.StoryDTO;
import com.melomarit.melopost.model.*;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostmortemService {
    private final PostmortemRepository repository;
    private final UserRepository userRepository;

    public PostmortemService(PostmortemRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public boolean isUserAdmin(User user) {
        return user.getRoles().contains("ADMIN");
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    public List<Postmortem> findAll() {
        User user = getCurrentUser();
        if (user == null || isUserAdmin(user)) {
            return repository.findAll();
        }
        Set<String> allowedDepts = user.getAllowedDepartments();
        return repository.findAll().stream()
                .filter(p -> allowedDepts.contains(p.getDepartment()))
                .collect(Collectors.toList());
    }

    public List<Postmortem> findRecent() {
        User user = getCurrentUser();
        // Since Cassandra doesn't support OrderBy across all rows without clustering keys easily,
        // we fetch all and sort in memory if the dataset is small, or use a better strategy.
        List<Postmortem> all = repository.findAll();
        if (user != null && !isUserAdmin(user)) {
            Set<String> allowedDepts = user.getAllowedDepartments();
            all = all.stream()
                    .filter(p -> allowedDepts.contains(p.getDepartment()))
                    .collect(Collectors.toList());
        }
        return all.stream()
                .sorted((p1, p2) -> {
                    if (p2.getCreatedAt() == null || p1.getCreatedAt() == null) return 0;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    public Postmortem findById(UUID id) {
        Postmortem pm = repository.findById(id).orElseThrow(() -> new RuntimeException("Postmortem not found"));
        User user = getCurrentUser();
        if (user != null && !isUserAdmin(user)) {
            if (pm.getDepartment() != null && !user.getAllowedDepartments().contains(pm.getDepartment())) {
                throw new RuntimeException("Access denied: You are not allowed to see postmortems from this department");
            }
        }
        return pm;
    }

    public Postmortem save(Postmortem postmortem) {
        // Ensure unique Incident Ref
        if (postmortem.getIncidentRef() != null && !postmortem.getIncidentRef().trim().isEmpty()) {
            List<Postmortem> existing = repository.findByIncidentRef(postmortem.getIncidentRef());
            for (Postmortem pm : existing) {
                if (!pm.getUuid().equals(postmortem.getUuid())) {
                    throw new RuntimeException("A postmortem with this Incident Ref already exists: " + postmortem.getIncidentRef());
                }
            }
        }

        // Since we moved to UDTs, we don't need separate repository calls for layers/holes/stories
        // Similarity check logic needs to be adapted or simplified
        return repository.save(postmortem);
    }

    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    public List<PostmortemSearchResultDTO> filter(String keyword, String type, String status, String incidentDate, String incidentDateOp, String incidentRef, String dueDate, String dueDateOp) {
        User user = getCurrentUser();
        List<Postmortem> all = repository.findAll();

        if (user != null && !isUserAdmin(user)) {
            Set<String> allowedDepts = user.getAllowedDepartments();
            all = all.stream()
                    .filter(p -> allowedDepts.contains(p.getDepartment()))
                    .collect(Collectors.toList());
        }

        List<PostmortemSearchResultDTO> results = new ArrayList<>();
        String kw = (keyword != null) ? keyword.toLowerCase().trim() : "";
        boolean hasKeyword = !kw.isEmpty();

        for (Postmortem p : all) {
            PostmortemSearchResultDTO result = new PostmortemSearchResultDTO(p);
            boolean match = true;

            // Type filter
            if (type != null && !type.isEmpty() && !type.equals(p.getType())) {
                match = false;
            }

            // Status filter
            if (match && status != null && !status.isEmpty() && !status.equals(p.getStatus())) {
                match = false;
            }

            // Incident Ref filter
            if (match && incidentRef != null && !incidentRef.isEmpty()) {
                if (p.getIncidentRef() == null || !p.getIncidentRef().toLowerCase().contains(incidentRef.toLowerCase())) {
                    match = false;
                }
            }

            // Incident Date filter
            if (match && incidentDate != null && !incidentDate.isEmpty()) {
                if (p.getIncidentDate() == null) {
                    match = false;
                } else {
                    java.time.LocalDate filterDate = java.time.LocalDate.parse(incidentDate);
                    java.time.LocalDate pmDate = p.getIncidentDate().toLocalDate();
                    if ("before".equals(incidentDateOp)) {
                        if (!pmDate.isBefore(filterDate)) match = false;
                    } else if ("after".equals(incidentDateOp)) {
                        if (!pmDate.isAfter(filterDate)) match = false;
                    } else { // default "on"
                        if (!pmDate.isEqual(filterDate)) match = false;
                    }
                }
            }

            // Due Date filter
            if (match && dueDate != null && !dueDate.isEmpty()) {
                if (p.getDueDate() == null) {
                    match = false;
                } else {
                    java.time.LocalDate filterDate = java.time.LocalDate.parse(dueDate);
                    java.time.LocalDate pmDate = p.getDueDate().toLocalDate();
                    if ("before".equals(dueDateOp)) {
                        if (!pmDate.isBefore(filterDate)) match = false;
                    } else if ("after".equals(dueDateOp)) {
                        if (!pmDate.isAfter(filterDate)) match = false;
                    } else { // default "on"
                        if (!pmDate.isEqual(filterDate)) match = false;
                    }
                }
            }

            // Keyword filter (if provided, must match in addition to other filters)
            if (match && hasKeyword) {
                boolean keywordMatch = false;
                if (containsIgnoreCase(p.getTitle(), kw)) { keywordMatch = true; result.addHint("Title"); }
                if (containsIgnoreCase(p.getDescription(), kw)) { keywordMatch = true; result.addHint("Description"); }
                if (containsIgnoreCase(p.getIncidentRef(), kw)) { keywordMatch = true; result.addHint("Incident Ref"); }
                if (containsIgnoreCase(p.getIncidentSource(), kw)) { keywordMatch = true; result.addHint("Incident Source"); }
                if (containsIgnoreCase(p.getStoryStore(), kw)) { keywordMatch = true; result.addHint("Story Store"); }
                if (containsIgnoreCase(p.getDepartment(), kw)) { keywordMatch = true; result.addHint("Department"); }
                if (containsIgnoreCase(p.getFailedApplication(), kw)) { keywordMatch = true; result.addHint("Failed Application"); }
                if (containsIgnoreCase(p.getNote(), kw)) { keywordMatch = true; result.addHint("Note"); }

                // Tags
                if (p.getTags() != null) {
                    for (String tag : p.getTags()) {
                        if (containsIgnoreCase(tag, kw)) { keywordMatch = true; result.addHint("Tag: " + tag); }
                    }
                }

                // Layers/Holes/Stories keyword search
                if (!keywordMatch && p.getLayers() != null) {
                    for (CheeseLayerUDT layer : p.getLayers()) {
                        if (containsIgnoreCase(layer.getName(), kw)) { keywordMatch = true; result.addHint("Layer: " + layer.getName()); }
                        if (layer.getHoles() != null) {
                            for (HoleUDT hole : layer.getHoles()) {
                                if (containsIgnoreCase(hole.getDescription(), kw)) { keywordMatch = true; result.addHint("Hole in " + layer.getName()); }
                                if (hole.getStory() != null) {
                                    if (containsIgnoreCase(hole.getStory().getStoryNumber(), kw)) { keywordMatch = true; result.addHint("Story #: " + hole.getStory().getStoryNumber()); }
                                }
                            }
                        }
                    }
                }
                
                if (!keywordMatch) {
                    match = false;
                }
            }

            if (match) {
                results.add(result);
            }
        }

        return results;
    }

    public List<PostmortemSearchResultDTO> search(String keyword) {
        User user = getCurrentUser();
        String kw = keyword.toLowerCase();
        List<Postmortem> all = repository.findAll();

        List<PostmortemSearchResultDTO> results = new ArrayList<>();

        for (Postmortem p : all) {
            PostmortemSearchResultDTO result = new PostmortemSearchResultDTO(p);
            boolean match = false;

            // Basic fields
            if (containsIgnoreCase(p.getTitle(), kw)) { match = true; result.addHint("Title"); }
            if (containsIgnoreCase(p.getDescription(), kw)) { match = true; result.addHint("Description"); }
            if (containsIgnoreCase(p.getIncidentRef(), kw)) { match = true; result.addHint("Incident Ref"); }
            if (containsIgnoreCase(p.getIncidentSource(), kw)) { match = true; result.addHint("Incident Source"); }
            if (containsIgnoreCase(p.getStoryStore(), kw)) { match = true; result.addHint("Story Store"); }
            if (containsIgnoreCase(p.getDepartment(), kw)) { match = true; result.addHint("Department"); }
            if (containsIgnoreCase(p.getFailedApplication(), kw)) { match = true; result.addHint("Failed Application"); }
            if (containsIgnoreCase(p.getNote(), kw)) { match = true; result.addHint("Note"); }

            // Tags
            if (p.getTags() != null) {
                for (String tag : p.getTags()) {
                    if (containsIgnoreCase(tag, kw)) { match = true; result.addHint("Tag: " + tag); }
                }
            }

            // UDTs: Layers
            if (p.getLayers() != null) {
                for (CheeseLayerUDT layer : p.getLayers()) {
                    if (containsIgnoreCase(layer.getName(), kw)) { match = true; result.addHint("Layer: " + layer.getName()); }
                    if (containsIgnoreCase(layer.getDescription(), kw)) { match = true; result.addHint("Layer " + layer.getName() + " Description"); }

                    // Holes
                    if (layer.getHoles() != null) {
                        for (HoleUDT hole : layer.getHoles()) {
                            if (containsIgnoreCase(hole.getDescription(), kw)) { match = true; result.addHint("Hole Description in " + layer.getName()); }
                            if (containsIgnoreCase(hole.getTeamName(), kw)) { match = true; result.addHint("Hole Team in " + layer.getName()); }
                            if (containsIgnoreCase(hole.getRemedialAction(), kw)) { match = true; result.addHint("Hole Remedial Action in " + layer.getName()); }
                            if (hole.getTags() != null) {
                                for (String hTag : hole.getTags()) {
                                    if (containsIgnoreCase(hTag, kw)) { match = true; result.addHint("Hole Tag: " + hTag + " in " + layer.getName()); }
                                }
                            }

                            // Stories
                            StoryUDT story = hole.getStory();
                            if (story != null) {
                                if (containsIgnoreCase(story.getStoryNumber(), kw)) { match = true; result.addHint("Story #: " + story.getStoryNumber()); }
                                if (containsIgnoreCase(story.getWhatToFix(), kw)) { match = true; result.addHint("Story " + story.getStoryNumber() + ": What to fix"); }
                                if (containsIgnoreCase(story.getNotes(), kw)) { match = true; result.addHint("Story " + story.getStoryNumber() + ": Notes"); }
                                if (containsIgnoreCase(story.getBacklogName(), kw)) { match = true; result.addHint("Story " + story.getStoryNumber() + ": Backlog"); }
                                if (story.getTags() != null) {
                                    for (String sTag : story.getTags()) {
                                        if (containsIgnoreCase(sTag, kw)) { match = true; result.addHint("Story Tag: " + sTag); }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // UDTs: Timeline
            if (p.getTimelineEvents() != null) {
                for (TimelineEventUDT event : p.getTimelineEvents()) {
                    if (containsIgnoreCase(event.getDescription(), kw)) { match = true; result.addHint("Timeline Event"); }
                }
            }

            // UDTs: Questions
            if (p.getQuestions() != null) {
                for (PostmortemQuestionUDT q : p.getQuestions()) {
                    if (containsIgnoreCase(q.getQuestion(), kw)) { match = true; result.addHint("Question"); }
                    if (containsIgnoreCase(q.getAnswer(), kw)) { match = true; result.addHint("Answer"); }
                }
            }

            if (match) {
                results.add(result);
            }
        }

        if (user != null && !isUserAdmin(user)) {
            Set<String> allowedDepts = user.getAllowedDepartments();
            return results.stream()
                    .filter(r -> allowedDepts.contains(r.getPostmortem().getDepartment()))
                    .collect(Collectors.toList());
        }
        return results;
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && source.toLowerCase().contains(target);
    }

    public List<Postmortem> findByType(String type) {
        return repository.findByType(type);
    }

    public List<HoleDTO> findAllHoles(String keyword, String teamName, String actionStatus) {
        List<Postmortem> all = findAll();
        List<HoleDTO> result = new ArrayList<>();

        for (Postmortem p : all) {
            if (p.getLayers() != null) {
                for (CheeseLayerUDT layer : p.getLayers()) {
                    if (layer.getHoles() != null) {
                        for (HoleUDT hole : layer.getHoles()) {
                            boolean match = true;
                            if (keyword != null && !keyword.isBlank()) {
                                String kw = keyword.toLowerCase();
                                boolean kwMatch = false;
                                if (hole.getDescription() != null && hole.getDescription().toLowerCase().contains(kw)) kwMatch = true;
                                if (hole.getRemedialAction() != null && hole.getRemedialAction().toLowerCase().contains(kw)) kwMatch = true;
                                if (p.getTitle() != null && p.getTitle().toLowerCase().contains(kw)) kwMatch = true;
                                if (layer.getName() != null && layer.getName().toLowerCase().contains(kw)) kwMatch = true;
                                if (hole.getTags() != null) {
                                    for (String tag : hole.getTags()) {
                                        if (tag.toLowerCase().contains(kw)) {
                                            kwMatch = true;
                                            break;
                                        }
                                    }
                                }
                                if (!kwMatch) match = false;
                            }
                            if (match && teamName != null && !teamName.isBlank()) {
                                if (hole.getTeamName() == null || !hole.getTeamName().equalsIgnoreCase(teamName)) {
                                    match = false;
                                }
                            }
                            if (match && actionStatus != null && !actionStatus.isBlank()) {
                                if (hole.getActionStatus() == null || !hole.getActionStatus().equalsIgnoreCase(actionStatus)) {
                                    match = false;
                                }
                            }

                            if (match) {
                                result.add(new HoleDTO(hole, p, layer.getName()));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<Postmortem> findMajorPostmortems(UUID localUuid) {
        // This was a join query, now we need to filter in-memory or change data model
        return repository.findAll().stream()
                .filter(p -> p.getLocalPostmortemUuids() != null && p.getLocalPostmortemUuids().contains(localUuid))
                .collect(Collectors.toList());
    }

    public List<StoryDTO> findAllStories(String keyword, String teamName, String status) {
        List<Postmortem> all = findAll();
        List<StoryDTO> result = new ArrayList<>();

        for (Postmortem p : all) {
            if (p.getLayers() != null) {
                for (CheeseLayerUDT layer : p.getLayers()) {
                    if (layer.getHoles() != null) {
                        for (HoleUDT hole : layer.getHoles()) {
                            StoryUDT story = hole.getStory();
                            if (story != null) {
                                boolean match = true;
                                if (keyword != null && !keyword.isBlank()) {
                                    String kw = keyword.toLowerCase();
                                    boolean kwMatch = false;
                                    if (story.getStoryNumber() != null && story.getStoryNumber().toLowerCase().contains(kw)) kwMatch = true;
                                    if (story.getWhatToFix() != null && story.getWhatToFix().toLowerCase().contains(kw)) kwMatch = true;
                                    if (story.getNotes() != null && story.getNotes().toLowerCase().contains(kw)) kwMatch = true;
                                    if (p.getTitle() != null && p.getTitle().toLowerCase().contains(kw)) kwMatch = true;
                                    if (story.getTags() != null) {
                                        for (String tag : story.getTags()) {
                                            if (tag.toLowerCase().contains(kw)) {
                                                kwMatch = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!kwMatch) match = false;
                                }
                                if (match && teamName != null && !teamName.isBlank()) {
                                    if (story.getTeamName() == null || !story.getTeamName().equalsIgnoreCase(teamName)) {
                                        match = false;
                                    }
                                }
                                if (match && status != null && !status.isBlank()) {
                                    if (story.getStatus() == null || !story.getStatus().equalsIgnoreCase(status)) {
                                        match = false;
                                    }
                                }

                                if (match) {
                                    result.add(new StoryDTO(story, p, hole.getDescription()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}

package com.melomarit.melopost.service;

import com.melomarit.melopost.model.*;
import com.melomarit.melopost.repository.HoleRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.StoryRepository;
import com.melomarit.melopost.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostmortemService {
    private final PostmortemRepository repository;
    private final StoryRepository storyRepository;
    private final HoleRepository holeRepository;
    private final UserRepository userRepository;

    public PostmortemService(PostmortemRepository repository, StoryRepository storyRepository, HoleRepository holeRepository, UserRepository userRepository) {
        this.repository = repository;
        this.storyRepository = storyRepository;
        this.holeRepository = holeRepository;
        this.userRepository = userRepository;
    }

    private boolean isUserAdmin(User user) {
        return user.getRoles().contains("ADMIN");
    }

    private User getCurrentUser() {
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
        if (user == null || isUserAdmin(user)) {
            return repository.findTop5ByOrderByCreatedAtDesc();
        }
        Set<String> allowedDepts = user.getAllowedDepartments();
        // Since we want recent ones, we might need a more efficient way if there are many postmortems
        // but for now, we'll filter the result of repository call.
        // Or we can fetch all filtered and then take top 5.
        return repository.findAll().stream()
                .filter(p -> allowedDepts.contains(p.getDepartment()))
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());
    }

    public Postmortem findById(Long id) {
        Postmortem pm = repository.findById(id).orElseThrow(() -> new RuntimeException("Postmortem not found"));
        User user = getCurrentUser();
        if (user != null && !isUserAdmin(user)) {
            if (!user.getAllowedDepartments().contains(pm.getDepartment())) {
                throw new RuntimeException("Access denied: You are not allowed to see postmortems from this department");
            }
        }
        return pm;
    }

    public Postmortem save(Postmortem postmortem) {
        // Before saving, check for similar stories
        if (postmortem.getLayers() != null) {
            for (CheeseLayer layer : postmortem.getLayers()) {
                if (layer.getHoles() != null) {
                    for (Hole hole : layer.getHoles()) {
                        Story story = hole.getStory();
                        if (story != null && story.getId() == null) {
                            checkAndFlagSimilarStory(story, hole);
                        }
                    }
                }
            }
        }
        return repository.save(postmortem);
    }

    private void checkAndFlagSimilarStory(Story story, Hole hole) {
        // Similarity check: same story number or similar "whatToFix"
        List<Story> similarStories = null;
        if (story.getStoryNumber() != null && !story.getStoryNumber().trim().isEmpty()) {
            similarStories = storyRepository.findByStoryNumber(story.getStoryNumber());
        } else if (story.getWhatToFix() != null && !story.getWhatToFix().trim().isEmpty()) {
            similarStories = storyRepository.findByWhatToFixContainingIgnoreCase(story.getWhatToFix());
        }

        if (similarStories != null && !similarStories.isEmpty()) {
            // Found similar story/stories
            Story originalStory = similarStories.get(0);
            Optional<Hole> originalHole = holeRepository.findByStoryId(originalStory.getId());
            
            String flag = "found before: " + (originalHole.isPresent() ? "Hole ID " + originalHole.get().getId() : "Story ID " + originalStory.getId());
            if (!story.getTags().contains(flag)) {
                story.getTags().add(flag);
            }
        }
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    public List<Postmortem> search(String keyword) {
        User user = getCurrentUser();
        if (user == null || isUserAdmin(user)) {
            return repository.search(keyword);
        }
        Set<String> allowedDepts = user.getAllowedDepartments();
        return repository.search(keyword).stream()
                .filter(p -> allowedDepts.contains(p.getDepartment()))
                .collect(Collectors.toList());
    }
}

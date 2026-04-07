package com.melomarit.melopost.service;

import com.melomarit.melopost.model.CheeseLayer;
import com.melomarit.melopost.model.Hole;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.Story;
import com.melomarit.melopost.repository.HoleRepository;
import com.melomarit.melopost.repository.PostmortemRepository;
import com.melomarit.melopost.repository.StoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class PostmortemService {
    private final PostmortemRepository repository;
    private final StoryRepository storyRepository;
    private final HoleRepository holeRepository;

    public PostmortemService(PostmortemRepository repository, StoryRepository storyRepository, HoleRepository holeRepository) {
        this.repository = repository;
        this.storyRepository = storyRepository;
        this.holeRepository = holeRepository;
    }

    public List<Postmortem> findAll() {
        return repository.findAll();
    }

    public List<Postmortem> findRecent() {
        return repository.findTop5ByOrderByCreatedAtDesc();
    }

    public Postmortem findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Postmortem not found"));
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
        return repository.search(keyword);
    }
}

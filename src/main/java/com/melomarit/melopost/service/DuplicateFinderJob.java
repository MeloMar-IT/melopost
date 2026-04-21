package com.melomarit.melopost.service;

import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.StoryUDT;
import com.melomarit.melopost.repository.PostmortemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

@Service
public class DuplicateFinderJob {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateFinderJob.class);

    @Autowired
    private PostmortemRepository postmortemRepository;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    /**
     * Scheduled job removed as requested.
     */
    // @Scheduled(fixedRate = 7200000)
    public void runDuplicateFinder() {
        logger.info("[JOB] Starting scheduled duplicate detection on Cassandra postmortems...");
        
        List<Postmortem> allPostmortems = postmortemRepository.findAll();
        findHoleDuplicates(allPostmortems);
        findStoryDuplicates(allPostmortems);
        
        logger.info("[JOB] Duplicate detection complete.");
    }

    public void findHoleDuplicates(List<Postmortem> allPostmortems) {
        List<HoleUDT> allHoles = new ArrayList<>();
        for (Postmortem pm : allPostmortems) {
            if (pm.getLayers() != null) {
                pm.getLayers().forEach(layer -> {
                    if (layer.getHoles() != null) {
                        allHoles.addAll(layer.getHoles());
                    }
                });
            }
        }
        
        logger.info("[JOB] Checking {} holes for duplicates...", allHoles.size());
        
        Set<UUID> processedUuids = new HashSet<>();
        int duplicateCount = 0;

        for (HoleUDT hole : allHoles) {
            if (processedUuids.contains(hole.getUuid())) continue;
            
            List<HoleUDT> duplicates = duplicateDetectionService.findDuplicateHoles(hole, allHoles);
            if (!duplicates.isEmpty()) {
                duplicateCount += duplicates.size();
                logger.warn("[JOB] Potential duplicates found for Hole UUID {}: {}", 
                    hole.getUuid(), 
                    duplicates.stream().map(HoleUDT::getUuid).toList());
                
                processedUuids.add(hole.getUuid());
                duplicates.forEach(d -> processedUuids.add(d.getUuid()));
            }
        }
        logger.info("[JOB] Finished checking holes. Found {} duplicate associations.", duplicateCount);
    }

    public void findStoryDuplicates(List<Postmortem> allPostmortems) {
        List<StoryUDT> allStories = new ArrayList<>();
        for (Postmortem pm : allPostmortems) {
            if (pm.getLayers() != null) {
                pm.getLayers().forEach(layer -> {
                    if (layer.getHoles() != null) {
                        layer.getHoles().forEach(hole -> {
                            if (hole.getStory() != null) {
                                allStories.add(hole.getStory());
                            }
                        });
                    }
                });
            }
        }
        
        logger.info("[JOB] Checking {} stories for duplicates...", allStories.size());
        
        Set<UUID> processedUuids = new HashSet<>();
        int duplicateCount = 0;

        for (StoryUDT story : allStories) {
            if (processedUuids.contains(story.getUuid())) continue;
            
            List<StoryUDT> duplicates = duplicateDetectionService.findDuplicateStories(story, allStories);
            if (!duplicates.isEmpty()) {
                duplicateCount += duplicates.size();
                logger.warn("[JOB] Potential duplicates found for Story UUID {}: {}", 
                    story.getUuid(), 
                    duplicates.stream().map(StoryUDT::getUuid).toList());
                
                processedUuids.add(story.getUuid());
                duplicates.forEach(d -> processedUuids.add(d.getUuid()));
            }
        }
        logger.info("[JOB] Finished checking stories. Found {} duplicate associations.", duplicateCount);
    }
}

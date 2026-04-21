package com.melomarit.melopost.service;

import com.melomarit.melopost.model.CheeseLayerUDT;
import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.Postmortem;
import com.melomarit.melopost.model.StoryUDT;
import com.melomarit.melopost.repository.PostmortemRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class DuplicateFinderJobTest {

    @Mock
    private PostmortemRepository postmortemRepository;

    @Mock
    private DuplicateDetectionService duplicateDetectionService;

    @InjectMocks
    private DuplicateFinderJob duplicateFinderJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRunDuplicateFinder_Success() {
        // Mock Postmortem with Holes and Stories
        Postmortem pm = new Postmortem();
        pm.setUuid(UUID.randomUUID());

        CheeseLayerUDT layer = new CheeseLayerUDT();
        layer.setName("Build");
        
        HoleUDT h1 = new HoleUDT(); h1.setUuid(UUID.randomUUID()); h1.setDescription("Hole 1");
        HoleUDT h2 = new HoleUDT(); h2.setUuid(UUID.randomUUID()); h2.setDescription("Hole 2");
        
        StoryUDT s1 = new StoryUDT(); s1.setUuid(UUID.randomUUID()); s1.setWhatToFix("Story 1");
        StoryUDT s2 = new StoryUDT(); s2.setUuid(UUID.randomUUID()); s2.setWhatToFix("Story 2");
        
        h1.setStory(s1);
        h2.setStory(s2);
        
        layer.setHoles(Arrays.asList(h1, h2));
        pm.setLayers(Collections.singletonList(layer));

        when(postmortemRepository.findAll()).thenReturn(Collections.singletonList(pm));

        // Mock Duplicate detection
        when(duplicateDetectionService.findDuplicateHoles(any(HoleUDT.class), anyList())).thenReturn(Collections.emptyList());
        when(duplicateDetectionService.findDuplicateStories(any(StoryUDT.class), anyList())).thenReturn(Collections.emptyList());

        // Run job
        duplicateFinderJob.runDuplicateFinder();

        // Verify repository was called
        verify(postmortemRepository, times(1)).findAll();
        
        // Each hole/story should trigger a check
        verify(duplicateDetectionService, atLeast(2)).findDuplicateHoles(any(HoleUDT.class), anyList());
        verify(duplicateDetectionService, atLeast(2)).findDuplicateStories(any(StoryUDT.class), anyList());
    }
}

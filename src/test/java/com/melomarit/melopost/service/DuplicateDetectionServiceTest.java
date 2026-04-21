package com.melomarit.melopost.service;

import com.melomarit.melopost.model.HoleUDT;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectionServiceTest {

    private DuplicateDetectionService duplicateDetectionService;

    @BeforeEach
    void setUp() {
        duplicateDetectionService = new DuplicateDetectionService();
    }

    @Test
    void testAreFuzzySimilar_ExactMatch() {
        assertTrue(duplicateDetectionService.areFuzzySimilar("Test description", "test description"));
    }

    @Test
    void testAreFuzzySimilar_HighSimilarity() {
        // "Database down" vs "Database is down"
        // L-distance: 3 ( " is")
        // Max length: 16
        // Similarity: 1 - 3/16 = 0.8125 >= 0.8
        assertTrue(duplicateDetectionService.areFuzzySimilar("Database down", "Database is down"));
    }

    @Test
    void testAreFuzzySimilar_LowSimilarity() {
        assertFalse(duplicateDetectionService.areFuzzySimilar("Database down", "Network latency"));
    }

    @Test
    void testFindDuplicateHoles() {
        HoleUDT hole = new HoleUDT();
        UUID uuid1 = UUID.randomUUID();
        hole.setUuid(uuid1);
        hole.setDescription("The server crashed");

        HoleUDT duplicate = new HoleUDT();
        UUID uuid2 = UUID.randomUUID();
        duplicate.setUuid(uuid2);
        duplicate.setDescription("The server crashed!");

        List<HoleUDT> result = duplicateDetectionService.findDuplicateHoles(hole, List.of(duplicate));

        assertEquals(1, result.size());
        assertEquals(uuid2, result.get(0).getUuid());
    }
}

package com.melomarit.melopost.service;

import com.melomarit.melopost.model.HoleUDT;
import com.melomarit.melopost.model.StoryUDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private static final double SIMILARITY_THRESHOLD = 0.8;

    public boolean areFuzzySimilar(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return false;
        }

        if (text1.equalsIgnoreCase(text2)) {
            return true;
        }

        double similarity = calculateLevenshteinSimilarity(text1, text2);
        return similarity >= SIMILARITY_THRESHOLD;
    }

    private double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1.toLowerCase(), s2.toLowerCase());
        int maxLength = Math.max(s1.length(), s2.length());
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public List<HoleUDT> findDuplicateHoles(HoleUDT hole, List<HoleUDT> existingHoles) {
        return existingHoles.stream()
                .filter(existing -> !existing.getUuid().equals(hole.getUuid()))
                .filter(existing -> areFuzzySimilar(hole.getDescription(), existing.getDescription()))
                .collect(Collectors.toList());
    }

    public List<StoryUDT> findDuplicateStories(StoryUDT story, List<StoryUDT> existingStories) {
        return existingStories.stream()
                .filter(existing -> !existing.getUuid().equals(story.getUuid()))
                .filter(existing -> areFuzzySimilar(story.getWhatToFix(), existing.getWhatToFix()))
                .collect(Collectors.toList());
    }
}

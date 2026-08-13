package com.ege.cvrag.eval;

import java.util.List;
import java.util.Objects;

/**
 * Pure scoring functions for the evaluation harness. No Spring, no I/O — so they
 * are cheap and deterministic to unit-test in CI, independent of the live stack.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
        // utility class
    }

    /** True if {@code expectedSection} appears anywhere in the retrieved list. */
    public static boolean hit(List<String> retrievedSections, String expectedSection) {
        return reciprocalRank(retrievedSections, expectedSection) > 0.0;
    }

    /**
     * Reciprocal rank of the expected section: 1/rank where rank is 1-based, or 0
     * if it was not retrieved. Higher is better; averaged across cases this is MRR.
     */
    public static double reciprocalRank(List<String> retrievedSections, String expectedSection) {
        for (int i = 0; i < retrievedSections.size(); i++) {
            if (retrievedSections.get(i).equalsIgnoreCase(expectedSection)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    /**
     * Fraction of {@code keywords} that appear (case-insensitively) in {@code answer}.
     * Returns 1.0 when there are no keywords to check.
     */
    public static double keywordCoverage(String answer, List<String> keywords) {
        if (Objects.isNull(keywords) || keywords.isEmpty()) {
            return 1.0;
        }
        String haystack = Objects.isNull(answer) ? "" : answer.toLowerCase();
        long matched = keywords.stream()
                .filter(k -> haystack.contains(k.toLowerCase()))
                .count();
        return (double) matched / keywords.size();
    }
}

package com.ege.cvrag.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RetrievalMetricsTest {

    @Test
    void reciprocalRankReflectsPosition() {
        List<String> retrieved = List.of("Skills", "Education", "Languages");

        assertThat(RetrievalMetrics.reciprocalRank(retrieved, "Skills")).isEqualTo(1.0);
        assertThat(RetrievalMetrics.reciprocalRank(retrieved, "Education")).isEqualTo(0.5);
        assertThat(RetrievalMetrics.reciprocalRank(retrieved, "Languages"))
                .isCloseTo(1.0 / 3.0, within(1e-9));
    }

    @Test
    void reciprocalRankIsZeroAndHitIsFalseWhenMissing() {
        List<String> retrieved = List.of("Skills", "Education");

        assertThat(RetrievalMetrics.reciprocalRank(retrieved, "Certificates")).isEqualTo(0.0);
        assertThat(RetrievalMetrics.hit(retrieved, "Certificates")).isFalse();
        assertThat(RetrievalMetrics.hit(retrieved, "Skills")).isTrue();
    }

    @Test
    void sectionMatchIsCaseInsensitive() {
        assertThat(RetrievalMetrics.hit(List.of("education"), "Education")).isTrue();
    }

    @Test
    void keywordCoverageCountsCaseInsensitiveMatches() {
        String answer = "Ege has 4 years of experience with PostgreSQL and Redis.";

        assertThat(RetrievalMetrics.keywordCoverage(answer, List.of("postgresql", "redis")))
                .isEqualTo(1.0);
        assertThat(RetrievalMetrics.keywordCoverage(answer, List.of("PostgreSQL", "Kafka")))
                .isEqualTo(0.5);
    }

    @Test
    void keywordCoverageIsOneWhenNoKeywords() {
        assertThat(RetrievalMetrics.keywordCoverage("anything", List.of())).isEqualTo(1.0);
        assertThat(RetrievalMetrics.keywordCoverage("anything", null)).isEqualTo(1.0);
    }
}

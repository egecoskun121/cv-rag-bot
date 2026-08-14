package com.ege.cvrag.qa;

import com.ege.cvrag.model.qa.QaEvent;
import com.ege.cvrag.model.qa.QaStatsSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class QaStatsAggregatorTest {

    private static QaEvent event(double topScore, long latencyMs, String... sections) {
        return new QaEvent("q", List.of(sections), topScore, "a", latencyMs, "2026-08-14T00:00:00Z");
    }

    @Test
    void aggregatesCountsLatencyAndSections() {
        QaStatsAggregator agg = new QaStatsAggregator(0.5);
        agg.record(event(0.9, 100, "Profile", "ilaBank"));
        agg.record(event(0.8, 300, "Profile"));

        QaStatsSnapshot snap = agg.snapshot();

        assertThat(snap.totalQuestions()).isEqualTo(2);
        assertThat(snap.weakRetrievals()).isZero();
        assertThat(snap.avgLatencyMs()).isEqualTo(200.0, within(0.001));
        assertThat(snap.topSections()).containsEntry("Profile", 2L).containsEntry("ilaBank", 1L);
    }

    @Test
    void countsWeakRetrievalsBelowThreshold() {
        QaStatsAggregator agg = new QaStatsAggregator(0.5);
        agg.record(event(0.42, 50, "Skills"));   // weak
        agg.record(event(0.77, 50, "Skills"));   // fine

        assertThat(agg.snapshot().weakRetrievals()).isEqualTo(1);
        assertThat(agg.isWeak(0.49)).isTrue();
        assertThat(agg.isWeak(0.50)).isFalse();
    }

    @Test
    void emptyAggregatorReportsZeroes() {
        QaStatsSnapshot snap = new QaStatsAggregator(0.5).snapshot();

        assertThat(snap.totalQuestions()).isZero();
        assertThat(snap.avgLatencyMs()).isZero();
        assertThat(snap.topSections()).isEmpty();
    }
}

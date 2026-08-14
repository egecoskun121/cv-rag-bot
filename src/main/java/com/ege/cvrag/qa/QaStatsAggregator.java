package com.ege.cvrag.qa;

import com.ege.cvrag.model.qa.QaEvent;
import com.ege.cvrag.model.qa.QaStatsSnapshot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Thread-safe, in-memory aggregation of {@link QaEvent}s. Written by the Kafka
 * consumer (possibly from several threads) and read by the stats endpoint, so all
 * counters are concurrent. In-memory keeps the showcase simple; a real deployment
 * would push these to a time-series store.
 */
@Component
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class QaStatsAggregator {

    private final double weakScoreThreshold;

    private final AtomicLong totalQuestions = new AtomicLong();
    private final AtomicLong weakRetrievals = new AtomicLong();
    private final AtomicLong latencySumMs = new AtomicLong();
    private final Map<String, LongAdder> sectionCounts = new ConcurrentHashMap<>();

    public QaStatsAggregator(@Value("${app.qa.weak-score-threshold:0.5}") double weakScoreThreshold) {
        this.weakScoreThreshold = weakScoreThreshold;
    }

    public void record(QaEvent event) {
        totalQuestions.incrementAndGet();
        latencySumMs.addAndGet(event.latencyMs());
        if (event.topScore() < weakScoreThreshold) {
            weakRetrievals.incrementAndGet();
        }
        event.retrievedSections()
                .forEach(section -> sectionCounts.computeIfAbsent(section, key -> new LongAdder()).increment());
    }

    public QaStatsSnapshot snapshot() {
        long total = totalQuestions.get();
        double avgLatency = total == 0 ? 0.0 : (double) latencySumMs.get() / total;
        Map<String, Long> sections = sectionCounts.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().sum()));
        return new QaStatsSnapshot(total, weakRetrievals.get(), avgLatency, sections);
    }

    public boolean isWeak(double topScore) {
        return topScore < weakScoreThreshold;
    }
}

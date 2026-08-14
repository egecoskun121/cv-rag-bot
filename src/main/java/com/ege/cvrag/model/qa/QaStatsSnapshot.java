package com.ege.cvrag.model.qa;

import java.util.Map;

/**
 * A point-in-time view of the aggregated Q&amp;A metrics, returned by
 * {@code GET /api/v1/qa/stats}. {@code weakRetrievals} counts answers whose best
 * similarity score fell below the configured threshold (a proxy for "we probably
 * couldn't ground this well"); {@code topSections} is how often each CV section
 * was retrieved.
 */
public record QaStatsSnapshot(long totalQuestions,
                              long weakRetrievals,
                              double avgLatencyMs,
                              Map<String, Long> topSections) {}

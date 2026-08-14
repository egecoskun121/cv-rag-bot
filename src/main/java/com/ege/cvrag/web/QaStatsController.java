package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.qa.QaStatsSnapshot;
import com.ege.cvrag.qa.QaStatsAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the aggregated Q&amp;A metrics collected from the event stream. Present
 * only when {@code app.qa.events.enabled=true} (same switch as the publisher and
 * consumer), so the whole observability feature turns on and off together.
 */
@RestController
@RequestMapping(RagBotConstants.API_V1)
@ConditionalOnProperty(prefix = "app.qa", name = "events.enabled", havingValue = "true")
public class QaStatsController {

    private final QaStatsAggregator stats;

    public QaStatsController(QaStatsAggregator stats) {
        this.stats = stats;
    }

    @GetMapping(RagBotConstants.QA_STATS_PATH)
    public ResponseEntity<QaStatsSnapshot> stats() {
        return ResponseEntity.ok(stats.snapshot());
    }
}

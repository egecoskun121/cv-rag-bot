package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.ingestion.DocumentIngestion;
import com.ege.cvrag.model.ingestion.IngestionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers a full re-index on demand, so content changes (e.g. an updated
 * {@code cv.md} in S3) are picked up without restarting the app. Returns the
 * per-source chunk breakdown from the same {@link DocumentIngestion#reindex()}
 * that runs at startup.
 */
@RestController
@RequestMapping(RagBotConstants.API_V1)
public class ReindexController {

    private static final Logger log = LoggerFactory.getLogger(ReindexController.class);

    private final DocumentIngestion ingestion;

    public ReindexController(DocumentIngestion ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping(RagBotConstants.REINDEX_PATH)
    public ResponseEntity<IngestionSummary> reindex() {
        log.info("Manual re-index requested");
        return ResponseEntity.ok(ingestion.reindex());
    }
}

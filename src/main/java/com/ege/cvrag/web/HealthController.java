package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness endpoint for monitoring/health checks. */
@RestController
public class HealthController {

    @GetMapping(RagBotConstants.HEALTH_PATH)
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(RagBotConstants.STATUS_UP));
    }
}

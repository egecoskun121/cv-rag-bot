package com.ege.cvrag.web;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * Translates infrastructure failures into clean HTTP responses instead of a raw
 * 500 stack trace. Each handler logs the cause (for us) and returns a small,
 * uniform {@link ErrorResponse} (for the client).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Ollama unreachable (connection refused / read timeout). */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleOllamaUnreachable(ResourceAccessException ex) {
        log.error("Ollama unreachable", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, RagBotConstants.ERROR_AI_UNAVAILABLE);
    }

    /** Ollama reachable but returned an error status, or the body was unusable. */
    @ExceptionHandler({RestClientException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleOllamaBadResponse(RuntimeException ex) {
        log.error("Ollama returned a bad response", ex);
        return build(HttpStatus.BAD_GATEWAY, RagBotConstants.ERROR_AI_BAD_RESPONSE);
    }

    /** Database / pgvector access failure. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        log.error("Data access failure", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, RagBotConstants.ERROR_DATA_ACCESS);
    }

    /** Anything else — never leak a stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, RagBotConstants.ERROR_UNEXPECTED);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message));
    }
}

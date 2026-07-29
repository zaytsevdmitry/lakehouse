package org.lakehouse.task.proxy.spark.controller;

import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SubmissionResponse> handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request handled: {}", ex.getMessage());
        SubmissionResponse response = new SubmissionResponse("ErrorResponse", ex.getMessage(), null, null, false);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SubmissionResponse> handleGenericException(Exception ex) {
        log.error("Internal proxy error occurred: ", ex);
        SubmissionResponse response = new SubmissionResponse("ErrorResponse", "Внутренняя ошибка прокси-сервиса: " + ex.getMessage(), null, null, false);
        return ResponseEntity.internalServerError().body(response);
    }
}

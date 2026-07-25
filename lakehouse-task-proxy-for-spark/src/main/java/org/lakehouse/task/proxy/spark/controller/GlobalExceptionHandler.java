package org.lakehouse.task.proxy.spark.controller;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CreateSubmissionResponse> handleBadRequest(IllegalArgumentException ex) {
        log.error("Bad request handled: {}", ex.getMessage());
        CreateSubmissionResponse response = new CreateSubmissionResponse("ErrorResponse", ex.getMessage(), null, null, false);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CreateSubmissionResponse> handleGenericException(Exception ex) {
        log.error("Internal proxy error occurred: ", ex);
        CreateSubmissionResponse response = new CreateSubmissionResponse("ErrorResponse", "Внутренняя ошибка прокси-сервиса: " + ex.getMessage(), null, null, false);
        return ResponseEntity.internalServerError().body(response);
    }
}

/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

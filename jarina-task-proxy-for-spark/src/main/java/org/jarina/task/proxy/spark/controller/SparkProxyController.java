/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.task.proxy.spark.controller;

import org.jarina.task.proxy.spark.dto.CreateSubmissionRequest;
import org.jarina.task.proxy.spark.dto.SubmissionResponse;
import org.jarina.task.proxy.spark.dto.SubmissionStatusResponse;
import org.jarina.task.proxy.spark.exception.ForbiddenException;
import org.jarina.task.proxy.spark.service.SparkProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/v1/submissions")
public class SparkProxyController {

    private static final Logger log = LoggerFactory.getLogger(SparkProxyController.class);

    private final SparkProxyService proxyService;

    public SparkProxyController(SparkProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @PostMapping("/create")
    public SubmissionResponse createSubmission(
            @RequestBody CreateSubmissionRequest request) {
        log.info("Received HTTP POST /create");
        return proxyService.create(request);
    }

    @GetMapping("/status/{submissionId}")
    public SubmissionStatusResponse getStatus(@PathVariable Long submissionId) {
        log.debug("Received HTTP GET /status/{}", submissionId);
        return proxyService.getStatus(submissionId);
    }

    @PostMapping("/kill/{submissionId}")
    public SubmissionResponse killSubmission(@PathVariable Long submissionId) {
        log.info("Received HTTP POST /kill/{}", submissionId);
        return proxyService.kill(submissionId);
    }

    @PostMapping("/killall")
    public SubmissionResponse killAllSubmissions() {
        log.warn("Received HTTP POST /killall");
        return proxyService.killAll();
    }

    @PostMapping("/clear")
    public SubmissionResponse clearCompleted(Authentication authentication) {
        requireControl(authentication);
        log.info("Received HTTP POST /clear");
        return proxyService.clear();
    }

    /**
     * Coarse CONTROL gate: Spark submissions do not carry a domain yet, so any
     * {@code <domain>:CONTROL} authority of the caller is accepted. Without it the
     * operation is rejected with HTTP 403 (see {@link GlobalExceptionHandler}).
     */
    private static void requireControl(Authentication authentication) {
        if (!hasControlRole(authentication)) {
            throw new ForbiddenException("Access denied: CONTROL role required for at least one domain");
        }
    }

    private static boolean hasControlRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
                .anyMatch(authority -> authority.toUpperCase(Locale.ROOT).endsWith(":CONTROL"));
    }
}

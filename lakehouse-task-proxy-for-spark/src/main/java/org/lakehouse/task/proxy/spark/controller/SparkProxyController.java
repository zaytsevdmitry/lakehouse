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

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.service.SparkProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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
    public SubmissionResponse clearCompleted() {
        log.info("Received HTTP POST /clear");
        return proxyService.clear();
    }
}

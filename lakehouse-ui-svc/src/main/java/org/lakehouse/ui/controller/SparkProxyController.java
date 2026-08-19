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
package org.lakehouse.ui.controller;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.ui.service.SparkProxyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spark-proxy")
public class SparkProxyController {

    private final SparkProxyService sparkProxyService;

    public SparkProxyController(SparkProxyService sparkProxyService) {
        this.sparkProxyService = sparkProxyService;
    }

    @GetMapping("/submissions")
    public SparkProxySubmissionsResponse getSubmissions(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        return sparkProxyService.getSubmissions(
                new SparkProxySubmissionsRequest(limit, lastId, id, status, dateFrom, dateTo));
    }

    @GetMapping("/submissions/{id}/spark-properties")
    public SparkProxySubmissionPropertiesDTO getSparkProperties(@PathVariable Long id) {
        return sparkProxyService.getSparkProperties(id);
    }

    @PostMapping("/submissions")
    public SubmissionResponse createSubmission(@RequestBody CreateSubmissionRequest request) {
        return sparkProxyService.createSubmission(request);
    }

    @GetMapping("/submissions/status/{submissionId}")
    public SubmissionStatusResponse getStatus(@PathVariable Long submissionId) {
        return sparkProxyService.getStatus(submissionId);
    }

    @PostMapping("/submissions/kill/{submissionId}")
    public SubmissionResponse killSubmission(@PathVariable Long submissionId) {
        return sparkProxyService.killSubmission(submissionId);
    }

    @PostMapping("/submissions/killall")
    public SubmissionResponse killAllSubmissions() {
        return sparkProxyService.killAllSubmissions();
    }

    @PostMapping("/submissions/clear")
    public SubmissionResponse clearCompleted() {
        return sparkProxyService.clearCompleted();
    }
}

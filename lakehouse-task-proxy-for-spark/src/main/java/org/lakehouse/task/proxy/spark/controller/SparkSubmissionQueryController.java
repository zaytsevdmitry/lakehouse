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

import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.service.SparkProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/spark-proxy-submissions")
public class SparkSubmissionQueryController {

    private static final Logger log = LoggerFactory.getLogger(SparkSubmissionQueryController.class);

    private final SparkProxyService proxyService;

    public SparkSubmissionQueryController(SparkProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping
    public SparkProxySubmissionsResponse getSubmissions(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {
        log.info("Received HTTP GET /api/v1/spark-proxy-submissions, limit={}, lastId={}, id={}, status={}, dateFrom={}, dateTo={}",
                limit, lastId, id, status, dateFrom, dateTo);
        return proxyService.getSubmissions(new SparkProxySubmissionsRequest(limit, lastId, id, status, dateFrom, dateTo));
    }

    @GetMapping("/{id}/spark-properties")
    public SparkProxySubmissionPropertiesDTO getSparkProperties(@PathVariable Long id) {
        log.info("Received HTTP GET /api/v1/spark-proxy-submissions/{}/spark-properties", id);
        return proxyService.getSparkProperties(id);
    }
}

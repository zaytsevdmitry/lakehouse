package org.lakehouse.task.proxy.spark.controller;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
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
    public CreateSubmissionResponse createSubmission(
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
    public CreateSubmissionResponse killSubmission(@PathVariable Long submissionId) {
        log.info("Received HTTP POST /kill/{}", submissionId);
        return proxyService.kill(submissionId);
    }

    @PostMapping("/killall")
    public CreateSubmissionResponse killAllSubmissions() {
        log.warn("Received HTTP POST /killall");
        return proxyService.killAll();
    }

    @PostMapping("/clear")
    public CreateSubmissionResponse clearCompleted() {
        log.info("Received HTTP POST /clear");
        return proxyService.clear();
    }
}

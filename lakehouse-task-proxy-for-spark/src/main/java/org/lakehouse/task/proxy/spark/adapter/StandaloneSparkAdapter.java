package org.lakehouse.task.proxy.spark.adapter;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.springframework.web.client.RestClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StandaloneSparkAdapter extends SparkAdapterBase {

    private static final Pattern SUBMISSION_ID_PATTERN = Pattern.compile("(driver-\\d{14}-\\d{4})");

    private final String restUrl;

    public StandaloneSparkAdapter(String masterUrl, String restUrl) {
        super(masterUrl);
        this.restUrl = restUrl;
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        Matcher matcher = SUBMISSION_ID_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new CreateErrorException("Standalone submission ID not found in output");
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        return defaultCreateSubmission(request);
    }

    @Override
    public CreateSubmissionResponse killSubmission(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            return restClient.post()
                    .uri("/v1/submissions/kill/" + submissionId)
                    .retrieve()
                    .body(CreateSubmissionResponse.class);
        } catch (Exception e) {
            log.error("Failed to kill standalone submission {}: {}", submissionId, e.getMessage(), e);
            return new CreateSubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    @Override
    public CreateSubmissionResponse killAllSubmissions() {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            return restClient.post()
                    .uri("/v1/submissions/killall")
                    .retrieve()
                    .body(CreateSubmissionResponse.class);
        } catch (Exception e) {
            log.error("Failed to kill all standalone submissions: {}", e.getMessage(), e);
            return new CreateSubmissionResponse("KillAllResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            SubmissionStatusResponse raw = restClient.get()
                    .uri("/v1/submissions/status/" + submissionId)
                    .retrieve()
                    .body(SubmissionStatusResponse.class);
            if (raw == null) {
                return new SubmissionStatusResponse("SparkStatusResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false, null, null, null);
            }

            return new SubmissionStatusResponse(
                    raw.action(),
                    raw.message(),
                    raw.serverSparkVersion(),
                    submissionId,
                    raw.success(),
                    raw.driverState(),
                    raw.workerId(),
                    raw.workerHostPort());
        } catch (Exception e) {
            log.error("Failed to get standalone submission status {}: {}", submissionId, e.getMessage(), e);
            return new SubmissionStatusResponse(
                    "SparkStatusResponse",
                    e.getMessage(),
                    null,
                    submissionId,
                    true,
                    ExternalStatus.UNKNOWN.name(),
                    null,
                    null);
        }
    }

    @Override
    public CreateSubmissionResponse clearCompleted() {
        RestClient restClient = RestClient.builder().baseUrl(restUrl).build();
        try {
            return restClient.post()
                    .uri("/v1/submissions/clear")
                    .retrieve()
                    .body(CreateSubmissionResponse.class);
        } catch (Exception e) {
            log.error("Failed to clear standalone submissions: {}", e.getMessage(), e);
            return new CreateSubmissionResponse("ClearResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }
}

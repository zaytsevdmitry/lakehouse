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
package org.lakehouse.task.proxy.spark.service;

import tools.jackson.databind.ObjectMapper;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsMeta;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SparkProxyService {

    private static final Logger log = LoggerFactory.getLogger(SparkProxyService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;

    public SparkProxyService(SparkSubmissionRepository repository,
                             SparkAdapter adapter,
                              TransactionTemplate transactionTemplate) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
    }

    public SubmissionResponse create(CreateSubmissionRequest request) {
        SparkSubmission submission = new SparkSubmission();
        submission.setAppResource(request.appResource());
        submission.setMainClass(request.mainClass());
        try {
            submission.setAppArgs(objectMapper.writeValueAsString(request.appArgs()));
        } catch (Exception e) {
            submission.setAppArgs("[]");
        }
        try {
            submission.setSparkProperties(objectMapper.writeValueAsString(request.sparkProperties()));
        } catch (Exception e) {
            submission.setSparkProperties("{}");
        }
        submission.setStatus(SparkSubmission.Status.WAITING);

        repository.save(submission);
        log.info("Created submission task id={}", submission.getId());

        return new SubmissionResponse(
                "CreateSubmissionResponse",
                ExternalStatus.WAITING.name(),
                null,
                String.valueOf(submission.getId()),
                true
        );
    }

    public SparkSubmission getSubmission(Long id) {
        return repository.findById(id).orElse(null);
    }

    public SparkProxySubmissionsResponse getSubmissions(SparkProxySubmissionsRequest request) {
        int limit = resolveLimit(request.limit());

        if (request.id() != null) {
            Optional<SparkSubmission> found = repository.findById(request.id());
            List<SparkProxySubmissionDTO> items = found.map(s -> List.of(toSubmissionDTO(s))).orElse(List.of());
            return new SparkProxySubmissionsResponse(items, new SparkProxySubmissionsMeta(limit, false, null));
        }

        SparkSubmission.Status status = null;
        if (request.status() != null && !request.status().isBlank()) {
            status = SparkSubmission.Status.valueOf(request.status());
        }
        Instant dateFrom = parseDate(request.dateFrom());
        Instant dateTo = parseDate(request.dateTo());

        List<SparkSubmission> rows = repository.findSubmissions(
                status, typed(dateFrom), typed(dateTo), typedLong(request.lastId()),
                PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "id")));

        boolean hasMore = rows.size() > limit;
        List<SparkSubmission> page = hasMore ? rows.subList(0, limit) : rows;
        Long nextCursor = hasMore ? page.get(page.size() - 1).getId() : null;

        List<SparkProxySubmissionDTO> items = page.stream().map(this::toSubmissionDTO).toList();
        return new SparkProxySubmissionsResponse(items, new SparkProxySubmissionsMeta(limit, hasMore, nextCursor));
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(value).toInstant();
    }

    private TypedParameterValue typed(Instant value) {
        return new TypedParameterValue(StandardBasicTypes.INSTANT, value);
    }

    private TypedParameterValue typedLong(Long value) {
        return new TypedParameterValue(StandardBasicTypes.LONG, value);
    }

    private SparkProxySubmissionDTO toSubmissionDTO(SparkSubmission s) {
        return new SparkProxySubmissionDTO(
                s.getId(),
                s.getSubmissionId(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getAppResource(),
                s.getMainClass(),
                parseJson(s.getAppArgs()),
                s.getMessage(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    public SparkProxySubmissionPropertiesDTO getSparkProperties(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return null;
        }
        return new SparkProxySubmissionPropertiesDTO(
                submission.getId(),
                submission.getSubmissionId(),
                parseJson(submission.getSparkProperties()));
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    public SubmissionStatusResponse getStatus(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return new SubmissionStatusResponse("StatusResponse", "NOT_FOUND", null, String.valueOf(id), false, "UNKNOWN", null, null);
        }

        String status = submission.getStatus().name();
        String externalStatus = ExternalStatus.fromInternal(status).name();

        return new SubmissionStatusResponse(
                "StatusResponse",
                submission.getMessage() != null ? submission.getMessage() : externalStatus,
                null,
                submission.getSubmissionId(),
                true,
                externalStatus,
                null, null
        );
    }

    public SubmissionResponse kill(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return new SubmissionResponse
                    ("KillResponse",
                            "NOT_FOUND",
                            null,
                            String.valueOf(id), false);
        }

        String realSubmissionId = submission.getSubmissionId();

        if (realSubmissionId == null) {
            repository.delete(submission);
            log.info("Deleted queued task id={}", id);
            return new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, String.valueOf(id), true);
        }

        SubmissionResponse result = adapter.killSubmission(realSubmissionId);
        if (Boolean.TRUE.equals(result.success())) {
            repository.delete(submission);
            log.info("Killed and deleted submission id={}, submissionId={}", id, realSubmissionId);
        }
        return result;
    }

    public SubmissionResponse killAll() {
        int deletedQueued = 0;

        for (SparkSubmission sub : repository.findByStatus(SparkSubmission.Status.WAITING)) {
            repository.delete(sub);
            deletedQueued++;
        }

        log.warn("Deleted {} queued tasks", deletedQueued);
        return new SubmissionResponse(
                "KillAllResponse",
                ExternalStatus.KILLED.name() + " " + deletedQueued + " queued tasks",
                null, null, true
        );
    }

    public SubmissionResponse clear() {
        record ClearResult(List<Long> toDelete, int killed) {}

        ClearResult result = transactionTemplate.execute(status -> {
            List<Long> ids = new ArrayList<>();
            int killCount = 0;
            List<Object[]> rows = repository.claimAllTasks(10000);
            for (Object[] row : rows) {
                Long id = ((Number) row[0]).longValue();
                String submissionId = (String) row[1];
                String statusStr = (String) row[2];

                if (submissionId == null) {
                    ids.add(id);
                    continue;
                }

                boolean isTerminal = statusStr != null && SparkSubmission.isFinalStatus(
                        SparkSubmission.Status.valueOf(statusStr));
                if (isTerminal) {
                    try {
                        SubmissionResponse r = adapter.clearCompleted(submissionId);
                        if (!Boolean.TRUE.equals(r.success())) {
                            log.warn("clearCompleted returned failure for submission {}: {}", submissionId, r.message());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to clearCompleted submission {}: {}", submissionId, e.getMessage());
                    }
                } else {
                    try {
                        SubmissionResponse r = adapter.killSubmission(submissionId);
                        if (Boolean.TRUE.equals(r.success())) {
                            killCount++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to kill submission {}: {}", submissionId, e.getMessage());
                    }
                }
                ids.add(id);
            }
            return new ClearResult(ids, killCount);
        });

        for (Long id : result.toDelete()) {
            repository.deleteById(id);
        }

        adapter.postClear();

        log.info("Clear completed: deleted {} records, killed={}", result.toDelete().size(), result.killed());
        return new SubmissionResponse("ClearResponse",
                "Cleared " + result.toDelete().size() + " submissions (killed " + result.killed() + ")", null, null, true);
    }
}

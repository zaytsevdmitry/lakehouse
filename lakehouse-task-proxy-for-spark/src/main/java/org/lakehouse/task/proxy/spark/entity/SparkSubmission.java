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
package org.lakehouse.task.proxy.spark.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "spark_submissions")
public class SparkSubmission {

    public enum Status {
        WAITING, SUBMITTED, RUNNING, FINISHED, KILLED, FAILED, ERROR, UNKNOWN
    }

    public static boolean isFinalStatus(Status status) {
        return status == Status.FINISHED || status == Status.KILLED || status == Status.FAILED || status == Status.ERROR;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", unique = true)
    private String submissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.WAITING;

    @Column(name = "app_resource")
    private String appResource;

    @Column(name = "main_class")
    private String mainClass;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "app_args")
    private String appArgs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spark_properties")
    private String sparkProperties;

    @Column(name = "message")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAppResource() { return appResource; }
    public void setAppResource(String appResource) { this.appResource = appResource; }

    public String getAppArgs() { return appArgs; }
    public void setAppArgs(String appArgs) { this.appArgs = appArgs; }

    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }

    public String getSparkProperties() { return sparkProperties; }
    public void setSparkProperties(String sparkProperties) { this.sparkProperties = sparkProperties; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

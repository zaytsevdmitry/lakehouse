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

    @Column(name = "cluster_type", nullable = false)
    private String clusterType;

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

    public String getClusterType() { return clusterType; }
    public void setClusterType(String clusterType) { this.clusterType = clusterType; }

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

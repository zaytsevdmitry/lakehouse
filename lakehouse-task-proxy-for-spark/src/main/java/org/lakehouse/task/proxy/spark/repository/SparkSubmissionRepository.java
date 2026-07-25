package org.lakehouse.task.proxy.spark.repository;

import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SparkSubmissionRepository extends JpaRepository<SparkSubmission, Long> {

    Optional<SparkSubmission> findBySubmissionId(String submissionId);

    List<SparkSubmission> findByStatus(SparkSubmission.Status status);

    @Query(value = """
            SELECT id, submission_id, cluster_type, app_resource, main_class, app_args, spark_properties
            FROM {h-schema}spark_submissions
            WHERE status = 'QUEUED'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Object[] claimNextTask();

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE {h-schema}spark_submissions
            SET status = 'CLAIMED',
                claimed_by = :instanceId,
                claimed_at = now()
            WHERE id = :id
            """, nativeQuery = true)
    void markClaimed(@Param("id") Long id, @Param("instanceId") String instanceId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE {h-schema}spark_submissions
            SET submission_id = :submissionId,
                status = :status,
                message = :message
            WHERE id = :id
            """, nativeQuery = true)
    void completeTask(@Param("id") Long id,
                      @Param("submissionId") String submissionId,
                      @Param("status") String status,
                      @Param("message") String message);

    Optional<SparkSubmission> findByIdAndSubmissionIdIsNotNull(Long id);
}

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
            WHERE status = 'WAITING'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Object[] claimNextTask();

    @Query(value = """
            SELECT id, submission_id, cluster_type
            FROM {h-schema}spark_submissions
            WHERE status NOT IN ('FINISHED', 'KILLED', 'FAILED', 'ERROR')
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Object[]> claimIncompleteTasks(@Param("batchSize") int batchSize);

    @Query(value = """
            SELECT id, submission_id, cluster_type
            FROM {h-schema}spark_submissions
            WHERE status IN ('FINISHED', 'KILLED', 'FAILED', 'ERROR')
              AND updated_at < now() - (:retentionSeconds || ' seconds')::interval
            ORDER BY updated_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Object[]> claimForCleanup(@Param("batchSize") int batchSize,
                                   @Param("retentionSeconds") long retentionSeconds);

    @Query(value = """
            SELECT id, submission_id, status
            FROM {h-schema}spark_submissions
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Object[]> claimAllTasks(@Param("batchSize") int batchSize);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE {h-schema}spark_submissions
            SET status = :status,
                message = :message
            WHERE id = :id
            """, nativeQuery = true)
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("message") String message);

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

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM {h-schema}spark_submissions
            WHERE id IN :ids
            """, nativeQuery = true)
    void deleteAllIds(@Param("ids") List<Long> ids);
}

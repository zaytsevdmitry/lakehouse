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
package org.lakehouse.task.proxy.spark.repository;

import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.hibernate.query.TypedParameterValue;
import org.springframework.data.domain.Pageable;
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
            SELECT id, submission_id, app_resource, main_class, app_args, spark_properties
            FROM {h-schema}spark_submissions
            WHERE status = 'WAITING'
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Object[] claimNextTask();

    @Query(value = """
            SELECT id, submission_id
            FROM {h-schema}spark_submissions
            WHERE status NOT IN ('FINISHED', 'KILLED', 'FAILED', 'ERROR')
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Object[]> claimIncompleteTasks(@Param("batchSize") int batchSize);

    @Query(value = """
            SELECT id, submission_id
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

    @Query("""
            SELECT s FROM SparkSubmission s
            WHERE (:status IS NULL OR s.status = :status)
              AND (cast(:dateFrom as timestamp) IS NULL OR s.createdAt >= :dateFrom)
              AND (cast(:dateTo as timestamp) IS NULL OR s.createdAt <= :dateTo)
              AND (cast(:lastId as long) IS NULL OR s.id < :lastId)
            """)
    List<SparkSubmission> findSubmissions(@Param("status") SparkSubmission.Status status,
                                          @Param("dateFrom") TypedParameterValue dateFrom,
                                          @Param("dateTo") TypedParameterValue dateTo,
                                          @Param("lastId") TypedParameterValue lastId,
                                          Pageable pageable);

    Optional<SparkSubmission> findByIdAndSubmissionIdIsNotNull(Long id);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM {h-schema}spark_submissions
            WHERE id IN :ids
            """, nativeQuery = true)
    void deleteAllIds(@Param("ids") List<Long> ids);
}

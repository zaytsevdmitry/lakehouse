package org.lakehouse.task.proxy.spark;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SparkSubmissionRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.jakarta.persistence.create-database-schemas", () -> "true");
    }

    @Autowired
    SparkSubmissionRepository repository;

    @Autowired
    EntityManager em;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private static SparkSubmission sub(SparkSubmission.Status status) {
        SparkSubmission s = new SparkSubmission();
        s.setStatus(status);
        return s;
    }

    @Nested
    class BasicCrud {

        @Test
        void saveAndFindById() {
            SparkSubmission s = new SparkSubmission();
            s.setAppResource("s3://bucket/app.jar");
            s.setMainClass("com.example.Main");
            s.setAppArgs("[\"--input\", \"/data\"]");
            s.setSparkProperties("{\"spark.cores\": \"2\"}");
            s.setStatus(SparkSubmission.Status.WAITING);
            SparkSubmission saved = repository.save(s);

            Optional<SparkSubmission> found = repository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(SparkSubmission.Status.WAITING);
            assertThat(found.get().getAppResource()).isEqualTo("s3://bucket/app.jar");
            assertThat(found.get().getMainClass()).isEqualTo("com.example.Main");
            assertThat(found.get().getAppArgs()).isEqualTo("[\"--input\", \"/data\"]");
            assertThat(found.get().getSparkProperties()).isEqualTo("{\"spark.cores\": \"2\"}");
        }

        @Test
        void findBySubmissionId() {
            SparkSubmission s = new SparkSubmission();
            s.setSubmissionId("driver-20260730-0001");
            s.setStatus(SparkSubmission.Status.RUNNING);
            repository.save(s);

            Optional<SparkSubmission> found = repository.findBySubmissionId("driver-20260730-0001");

            assertThat(found).isPresent();
            assertThat(found.get().getSubmissionId()).isEqualTo("driver-20260730-0001");
        }

        @Test
        void findBySubmissionId_returnsEmptyWhenNotFound() {
            Optional<SparkSubmission> found = repository.findBySubmissionId("nonexistent");

            assertThat(found).isEmpty();
        }

        @Test
        void findByStatus() {
            repository.save(sub(SparkSubmission.Status.WAITING));
            repository.save(sub(SparkSubmission.Status.RUNNING));
            repository.save(sub(SparkSubmission.Status.FINISHED));

            assertThat(repository.findByStatus(SparkSubmission.Status.WAITING)).hasSize(1);
            assertThat(repository.findByStatus(SparkSubmission.Status.RUNNING)).hasSize(1);
            assertThat(repository.findByStatus(SparkSubmission.Status.FINISHED)).hasSize(1);
        }

        @Test
        void findByIdAndSubmissionIdIsNotNull() {
            SparkSubmission withId = new SparkSubmission();
            withId.setSubmissionId("driver-001");
            withId.setStatus(SparkSubmission.Status.RUNNING);
            SparkSubmission savedWith = repository.save(withId);

            SparkSubmission withoutId = new SparkSubmission();
            withoutId.setStatus(SparkSubmission.Status.WAITING);
            repository.save(withoutId);

            Optional<SparkSubmission> found = repository.findByIdAndSubmissionIdIsNotNull(savedWith.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(savedWith.getId());

            assertThat(repository.findByIdAndSubmissionIdIsNotNull(99999L)).isEmpty();
        }
    }

    @Nested
    class ClaimNextTask {

        @Test
        void returnsEarliestWaiting() {
            repository.save(sub(SparkSubmission.Status.WAITING));
            repository.save(sub(SparkSubmission.Status.WAITING));

            Object[] claimed = extractFlatColumns(repository.claimNextTask());

            assertThat(claimed).isNotNull();
            assertThat(((Number) claimed[0]).longValue()).isPositive();
        }

        @Test
        void returnsNullWhenNoWaiting() {
            repository.save(sub(SparkSubmission.Status.RUNNING));

            Object r = repository.claimNextTask();

            assertThat(r).satisfiesAnyOf(
                    v -> assertThat(v).isNull(),
                    v -> assertThat((Object[]) v).isEmpty()
            );
        }
    }

    @Nested
    class ClaimIncompleteTasks {

        @Test
        void returnsNonTerminalRows() {
            repository.save(sub(SparkSubmission.Status.WAITING));
            repository.save(sub(SparkSubmission.Status.SUBMITTED));
            repository.save(sub(SparkSubmission.Status.RUNNING));
            repository.save(sub(SparkSubmission.Status.FINISHED));
            repository.save(sub(SparkSubmission.Status.KILLED));

            List<Object[]> rows = repository.claimIncompleteTasks(10);

            assertThat(rows).hasSize(3);
            assertThat(rows).allMatch(r -> r.length == 2);
        }

        @Test
        void respectsBatchSize() {
            for (int i = 0; i < 5; i++) {
                repository.save(sub(SparkSubmission.Status.WAITING));
            }

            List<Object[]> rows = repository.claimIncompleteTasks(2);

            assertThat(rows).hasSize(2);
        }

        @Test
        void returnsEmptyWhenAllTerminal() {
            repository.save(sub(SparkSubmission.Status.FINISHED));
            repository.save(sub(SparkSubmission.Status.FAILED));

            List<Object[]> rows = repository.claimIncompleteTasks(10);

            assertThat(rows).isEmpty();
        }
    }

    @Nested
    class ClaimForCleanup {

        @Test
        void returnsOldTerminalRows() {
            SparkSubmission s1 = sub(SparkSubmission.Status.FINISHED);
            s1.setUpdatedAt(Instant.now().minusSeconds(600));
            repository.save(s1);

            SparkSubmission s2 = sub(SparkSubmission.Status.KILLED);
            s2.setUpdatedAt(Instant.now().minusSeconds(600));
            repository.save(s2);

            List<Object[]> rows = repository.claimForCleanup(10, 300);

            assertThat(rows).hasSize(2);
        }

        @Test
        void excludesRecentTerminalRows() {
            SparkSubmission s = sub(SparkSubmission.Status.FINISHED);
            s.setUpdatedAt(Instant.now());
            repository.save(s);

            List<Object[]> rows = repository.claimForCleanup(10, 3600);

            assertThat(rows).isEmpty();
        }

        @Test
        void excludesNonTerminalRows() {
            SparkSubmission s = sub(SparkSubmission.Status.RUNNING);
            s.setUpdatedAt(Instant.now().minusSeconds(600));
            repository.save(s);

            List<Object[]> rows = repository.claimForCleanup(10, 300);

            assertThat(rows).isEmpty();
        }
    }

    @Nested
    class ClaimAllTasks {

        @Test
        void returnsAllRows() {
            repository.save(sub(SparkSubmission.Status.WAITING));
            repository.save(sub(SparkSubmission.Status.RUNNING));
            repository.save(sub(SparkSubmission.Status.FINISHED));

            List<Object[]> rows = repository.claimAllTasks(10);

            assertThat(rows).hasSize(3);
            assertThat(rows).allMatch(r -> r.length == 3);
        }

        @Test
        void respectsBatchSize() {
            for (int i = 0; i < 7; i++) {
                repository.save(sub(SparkSubmission.Status.WAITING));
            }

            List<Object[]> rows = repository.claimAllTasks(3);

            assertThat(rows).hasSize(3);
        }
    }

    @Nested
    class UpdateOperations {

        @Test
        void updateStatus() {
            SparkSubmission s = repository.save(sub(SparkSubmission.Status.WAITING));

            repository.updateStatus(s.getId(), "RUNNING", "Job started");
            em.clear();

            SparkSubmission updated = repository.findById(s.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(SparkSubmission.Status.RUNNING);
            assertThat(updated.getMessage()).isEqualTo("Job started");
        }

        @Test
        void completeTask() {
            SparkSubmission s = repository.save(sub(SparkSubmission.Status.WAITING));

            repository.completeTask(s.getId(), "driver-20260730-9999", "FINISHED", "Completed successfully");
            em.clear();

            SparkSubmission updated = repository.findById(s.getId()).orElseThrow();
            assertThat(updated.getSubmissionId()).isEqualTo("driver-20260730-9999");
            assertThat(updated.getStatus()).isEqualTo(SparkSubmission.Status.FINISHED);
            assertThat(updated.getMessage()).isEqualTo("Completed successfully");
        }
    }

    @Nested
    class DeleteOperations {

        @Test
        void deleteAllIds() {
            SparkSubmission s1 = repository.save(sub(SparkSubmission.Status.FINISHED));
            SparkSubmission s2 = repository.save(sub(SparkSubmission.Status.FINISHED));
            SparkSubmission s3 = repository.save(sub(SparkSubmission.Status.FINISHED));

            repository.deleteAllIds(List.of(s1.getId(), s3.getId()));
            em.clear();

            assertThat(repository.findById(s1.getId())).isEmpty();
            assertThat(repository.findById(s2.getId())).isPresent();
            assertThat(repository.findById(s3.getId())).isEmpty();
        }

        @Test
        void deleteAllIds_emptyListDoesNothing() {
            SparkSubmission s = repository.save(sub(SparkSubmission.Status.FINISHED));

            repository.deleteAllIds(List.of());
            em.clear();

            assertThat(repository.findById(s.getId())).isPresent();
        }
    }

    private static Object[] extractFlatColumns(Object r) {
        if (r == null) {
            return null;
        }
        if (r instanceof Object[][] && ((Object[][]) r).length > 0) {
            return ((Object[][]) r)[0];
        }
        if (r instanceof Object[] && ((Object[]) r).length > 0 && ((Object[]) r)[0] instanceof Object[]) {
            return (Object[]) ((Object[]) r)[0];
        }
        return (Object[]) r;
    }
}

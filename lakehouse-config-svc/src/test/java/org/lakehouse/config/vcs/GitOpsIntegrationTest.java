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

package org.lakehouse.config.vcs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.config.vcs.component.GitOpsScheduler;
import org.lakehouse.config.vcs.entity.VcsSyncLog;
import org.lakehouse.config.vcs.entity.VcsSyncStatus;
import org.lakehouse.config.vcs.repository.VcsSyncLogRepository;
import org.lakehouse.config.entities.dataset.DataSetSource;

import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.test.configutation.RestManipulator;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.test.config.util.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End to end synchronization test: a real Git repository is pushed into and the whole
 * GitOps pipeline (GitVcsClient, change set builder, synchronizer, scheduler) is exercised
 * against the real database. Tests are ordered because each synchronization cycle advances
 * the state of the repository and the database.
 */
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"lakehouse.security.enabled=false"})
@ComponentScan(basePackages = {"org.lakehouse.config", "org.lakehouse.test"},
        basePackageClasses = {JinJavaConfiguration.class})
@Import({FileLoader.class, RestManipulator.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitOpsIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test").withUsername("name").withPassword("password");
    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    private static TestGitRepository repository;

    @Autowired
    GitOpsScheduler scheduler;
    @Autowired
    VcsSyncLogRepository vcsSyncLogRepository;
    @Autowired
    org.lakehouse.config.repository.datasource.DriverRepository driverRepository;
    @Autowired
    DataSourceRepository dataSourceRepository;
    @Autowired
    DataSetRepository dataSetRepository;
    @Autowired
    DataSetSourceRepository dataSetSourceRepository;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    org.lakehouse.config.repository.ScheduleRepository scheduleRepository;

    @BeforeAll
    static void beforeAll() {
        kafka.start();
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.config.produce.kafka.producer.properties.bootstrap.servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        repository = TestGitRepository.create(tempDir());
        registry.add("lakehouse.config.vcs.domains.platform.priority", () -> "0");
        registry.add("lakehouse.config.vcs.domains.platform.git.repository-url", repository::bareUri);
        registry.add("lakehouse.config.vcs.domains.platform.git.branch", () -> "main");
        registry.add("lakehouse.config.vcs.domains.platform.git.local-clone-path", () -> repository.clonePath().toString());
        registry.add("lakehouse.config.vcs.git.sync.enabled", () -> "true");
        registry.add("lakehouse.config.vcs.git.sync.interval-ms", () -> "36000000");
        registry.add("lakehouse.config.vcs.git.sync.initial-delay-ms", () -> "36000000");
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("gitops-integration-test");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @Order(1)
    void appliesDriverCommitAndRecordsSuccess() throws Exception {
        String head = commit("config/drv-1.yaml", driver("vcs-drv-1", "first driver"));

        scheduler.sync();

        assertThat(driverRepository.findById("vcs-drv-1")).isPresent();
        assertThat(driverRepository.findById("vcs-drv-1").orElseThrow().isVcsManaged()).isTrue();
        assertThat(driverRepository.findById("vcs-drv-1").orElseThrow()
                .getDomainKeyName()).isEqualTo("platform");
        assertThat(logFor(head)).isPresent();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);

        // a VCS-managed construct cannot be changed or deleted through the REST API
        mockMvc.perform(delete("/v1_0/configs/drivers/vcs-drv-1"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/v1_0/configs/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keyName":"vcs-drv-1","description":"must be rejected"}
                                """))
                .andExpect(status().isConflict());
        assertThat(driverRepository.findById("vcs-drv-1")).isPresent();
    }

    @Test
    @Order(2)
    void appliesDependentDataSetsWithinOneCommit() {
        commit("config/vcs-ds.yaml", datasource());
        Map<String, String> dataSets = new LinkedHashMap<>();
        // dataset-b is pushed before dataset-a so that only the dependency ordering, not the
        // file ordering, can make the commit valid.
        dataSets.put("config/aa-dataset-b.yaml",
                datasetWithSource("dataset-b", "vcs-ds", "dataset-a"));
        dataSets.put("config/zz-dataset-a.yaml", dataset("dataset-a", "vcs-ds"));
        String head = commitAll(dataSets, "add dependent datasets");

        scheduler.sync();

        assertThat(dataSetRepository.findById("dataset-a").orElseThrow().isVcsManaged()).isTrue();
        assertThat(dataSetRepository.findById("dataset-b").orElseThrow().isVcsManaged()).isTrue();
        assertThat(dataSetRepository.findById("dataset-b").orElseThrow()
                .getDomainKeyName()).isEqualTo("platform");
        assertThat(dataSetSourceRepository.findByDataSetKeyName("dataset-b"))
                .extracting(DataSetSource::getSource)
                .anyMatch(source -> "dataset-a".equals(source.getKeyName()));
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);
    }

    @Test
    @Order(3)
    void resyncOfAppliedHeadIsSkipped() {
        long logCount = vcsSyncLogRepository.count();

        scheduler.sync();

        assertThat(vcsSyncLogRepository.count()).isEqualTo(logCount);
    }

    @Test
    @Order(4)
    void deleteOnlyUnmanagesTheConstruct() throws Exception {
        String head = repository.deleteFile("config/drv-1.yaml", "remove first driver");

        scheduler.sync();

        // the construct itself is retained in the database; only the VCS management
        // flag is cleared - the user deletes it through the REST API afterwards
        assertThat(driverRepository.findById("vcs-drv-1")).isPresent();
        assertThat(driverRepository.findById("vcs-drv-1").orElseThrow().isVcsManaged()).isFalse();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);

        // once un-managed, the REST API accepts the deletion
        mockMvc.perform(delete("/v1_0/configs/drivers/vcs-drv-1"))
                .andExpect(status().isAccepted());
        assertThat(driverRepository.findById("vcs-drv-1")).isEmpty();
    }

    @Test
    @Order(5)
    void invalidCommitIsRolledBackAndRecordedAsFailed() {
        Map<String, String> commit = new LinkedHashMap<>();
        commit.put("config/vcs-drv-3.yaml", driver("vcs-drv-3", "driver of a broken commit"));
        commit.put("config/zz-broken.yaml", dataset("vcs-broken", "missing-ds"));
        String head = commitAll(commit, "add commit that violates a foreign key");

        scheduler.sync();

        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.FAILED);
        assertThat(logFor(head).orElseThrow().getErrorMessage()).isNotBlank();
        // the whole commit is rolled back, including the valid driver of the same commit
        assertThat(driverRepository.findById("vcs-drv-3")).isEmpty();
        assertThat(dataSetRepository.findById("vcs-broken")).isEmpty();

        // a failed commit is never retried on its own
        scheduler.sync();
        assertThat(vcsSyncLogRepository.findAll().stream()
                .filter(log -> head.equals(log.getCommitId()) && log.getStatus() == VcsSyncStatus.FAILED)
                .count()).isEqualTo(1);

        // removing the offending files lets the next cycle succeed
        String cleanup = deleteFiles(List.of("config/vcs-drv-3.yaml", "config/zz-broken.yaml"),
                "remove broken files");
        cleanup(cleanup);
        assertThat(driverRepository.findById("vcs-drv-3")).isEmpty();
    }

    @Test
    @Order(6)
    void unknownKindCommitIsRecordedAsFailedAndSkipped() {
        String head = commit("config/unknown.yaml", "kind: NoSuchKind\nkeyName: anything\n");

        scheduler.sync();

        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.FAILED);
        assertThat(driverRepository.findById("anything")).isEmpty();

        scheduler.sync();
        assertThat(vcsSyncLogRepository.findAll().stream()
                .filter(log -> head.equals(log.getCommitId()) && log.getStatus() == VcsSyncStatus.FAILED)
                .count()).isEqualTo(1);

        cleanup(repository.deleteFile("config/unknown.yaml", "remove unknown kind file"));
    }

    @Test
    @Order(7)
    void updateModifiesTheExistingConstruct() {
        assertThat(dataSourceRepository.findById("vcs-ds")).isPresent();
        String head = commit("config/vcs-ds.yaml", datasource("updated description"));

        scheduler.sync();

        assertThat(dataSourceRepository.findById("vcs-ds").orElseThrow()
                .getDescription()).isEqualTo("updated description");
        assertThat(dataSourceRepository.findById("vcs-ds").orElseThrow().isVcsManaged()).isTrue();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);
    }

    @Test
    @Order(8)
    void scheduleReferencingAnotherDomainDataSetIsRejected() {
        // a data set owned by another configuration domain: as if it had been pushed by
        // the analytics repository rather than by the platform one being synchronized
        DataSet foreignDataSet = new DataSet();
        foreignDataSet.setKeyName("foreign-domain-dataset");
        foreignDataSet.setDescription("data set owned by the analytics domain");
        foreignDataSet.setDomainKeyName("analytics");
        foreignDataSet.setDataSource(dataSourceRepository.findById("vcs-ds").orElseThrow());
        foreignDataSet.setDatabaseSchemaName("test_schema");
        foreignDataSet.setTableName("foreign_table");
        foreignDataSet.setVcsManaged(true);
        dataSetRepository.save(foreignDataSet);

        String head = commit("config/foreign-schedule.yaml", """
                kind: Schedule
                keyName: foreign-schedule
                description: schedule referencing a foreign data set
                intervalExpression: "@daily"
                startDateTime: "2025-01-01T00:00:00.0+00:00"
                enabled: true
                scenarioActs:
                  - name: foreign_act
                    dataSetKeyName: foreign-domain-dataset
                    intervalStart: "2025-01-01T00:00:00.0+00:00"
                    intervalEnd: "2025-01-02T00:00:00.0+00:00"
                scenarioActEdges: []
                """);

        scheduler.sync();

        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.FAILED);
        assertThat(logFor(head).orElseThrow().getErrorMessage())
                .contains("foreign-schedule").contains("foreign-domain-dataset")
                .contains("platform").contains("analytics");
        // the whole commit is rolled back: the schedule must not be persisted
        assertThat(scheduleRepository.findById("foreign-schedule")).isEmpty();
    }

    @Test
    @Order(9)
    void scheduleReferencingOwnDomainDataSetIsApplied() {
        // the foreign schedule of the previous commit was never applied, but since its
        // commit never succeeded it still blocks the next change set diff: remove it first
        cleanup(repository.deleteFile("config/foreign-schedule.yaml", "remove foreign schedule"));

        String head = commit("config/own-schedule.yaml", """
                kind: Schedule
                keyName: own-schedule
                description: schedule referencing a data set of its own domain
                intervalExpression: "@daily"
                startDateTime: "2025-01-01T00:00:00.0+00:00"
                enabled: true
                scenarioActs:
                  - name: own_act
                    dataSetKeyName: dataset-a
                    intervalStart: "2025-01-01T00:00:00.0+00:00"
                    intervalEnd: "2025-01-02T00:00:00.0+00:00"
                scenarioActEdges: []
                """);

        scheduler.sync();

        assertThat(scheduleRepository.findById("own-schedule")).isPresent();
        assertThat(scheduleRepository.findById("own-schedule").orElseThrow().isVcsManaged()).isTrue();
        assertThat(scheduleRepository.findById("own-schedule").orElseThrow())
                .extracting(Schedule::getDomainKeyName).isEqualTo("platform");
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);
    }

    private void cleanup(String head) {
        scheduler.sync();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);
    }

    private String commit(String path, String content) {
        return repository.commitFile(path, content, "commit " + path);
    }

    private String commitAll(Map<String, String> files, String message) {
        return repository.commitAll(files, message);
    }

    private String deleteFiles(Iterable<String> paths, String message) {
        return repository.deleteFiles(paths, message);
    }

    private Optional<VcsSyncLog> logFor(String commitId) {
        return vcsSyncLogRepository.findAll().stream()
                .filter(log -> commitId.equals(log.getCommitId()))
                .findFirst();
    }

    private String driver(String key, String description) {
        return """
                kind: Driver
                keyName: %s
                description: %s
                """.formatted(key, description);
    }

    private String datasource() {
        return datasource("gitops test datasource");
    }

    private String datasource(String description) {
        return """
                kind: DataSource
                keyName: vcs-ds
                description: %s
                databaseProtocol: postgresql
                dataSourceType: database
                service:
                  host: localhost
                  port: "5432"
                  urn: testdb
                """.formatted(description);
    }

    private String dataset(String key, String dataSource) {
        return """
                kind: DataSet
                keyName: %s
                dataSourceKeyName: %s
                databaseSchemaName: test_schema
                tableName: %s
                description: gitops test data set
                sources: {}
                """.formatted(key, dataSource, key);
    }

    private String datasetWithSource(String key, String dataSource, String sourceKey) {
        return """
                kind: DataSet
                keyName: %s
                dataSourceKeyName: %s
                databaseSchemaName: test_schema
                tableName: %s
                description: gitops test data set
                sources:
                  %s:
                    properties:
                      kind: initial
                """.formatted(key, dataSource, key, sourceKey);
    }
}
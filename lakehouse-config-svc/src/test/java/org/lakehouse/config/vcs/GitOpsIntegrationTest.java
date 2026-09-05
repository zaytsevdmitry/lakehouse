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
import org.lakehouse.config.repository.NameSpaceRepository;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
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
    NameSpaceRepository nameSpaceRepository;
    @Autowired
    DataSourceRepository dataSourceRepository;
    @Autowired
    DataSetRepository dataSetRepository;
    @Autowired
    DataSetSourceRepository dataSetSourceRepository;
    @Autowired
    MockMvc mockMvc;

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
        registry.add("lakehouse.config.schedule.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        repository = TestGitRepository.create(tempDir());
        registry.add("lakehouse.config.vcs.git.repository-url", repository::bareUri);
        registry.add("lakehouse.config.vcs.git.branch", () -> "main");
        registry.add("lakehouse.config.vcs.git.local-clone-path", () -> repository.clonePath().toString());
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
    void appliesNamespaceCommitAndRecordsSuccess() throws Exception {
        String head = commit("config/ns-1.yaml", namespace("vcs-ns-1", "first namespace"));

        scheduler.sync();

        assertThat(nameSpaceRepository.findById("vcs-ns-1")).isPresent();
        assertThat(nameSpaceRepository.findById("vcs-ns-1").orElseThrow().isVcsManaged()).isTrue();
        assertThat(logFor(head)).isPresent();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);

        // a VCS-managed construct cannot be changed or deleted through the REST API
        mockMvc.perform(delete("/v1_0/configs/nameSpaces/vcs-ns-1"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/v1_0/configs/nameSpaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"keyName":"vcs-ns-1","description":"must be rejected"}
                                """))
                .andExpect(status().isConflict());
        assertThat(nameSpaceRepository.findById("vcs-ns-1")).isPresent();
    }

    @Test
    @Order(2)
    void appliesDependentDataSetsWithinOneCommit() {
        commit("config/vcs-ns-2.yaml", namespace("vcs-ns-2", "second namespace"));
        commit("config/vcs-ds.yaml", datasource());
        Map<String, String> dataSets = new LinkedHashMap<>();
        // dataset-b is pushed before dataset-a so that only the dependency ordering, not the
        // file ordering, can make the commit valid.
        dataSets.put("config/aa-dataset-b.yaml",
                datasetWithSource("dataset-b", "vcs-ns-2", "vcs-ds", "dataset-a"));
        dataSets.put("config/zz-dataset-a.yaml", dataset("dataset-a", "vcs-ns-2", "vcs-ds"));
        String head = commitAll(dataSets, "add dependent datasets");

        scheduler.sync();

        assertThat(nameSpaceRepository.findById("vcs-ns-2").orElseThrow().isVcsManaged()).isTrue();
        assertThat(dataSetRepository.findById("dataset-a").orElseThrow().isVcsManaged()).isTrue();
        assertThat(dataSetRepository.findById("dataset-b").orElseThrow().isVcsManaged()).isTrue();
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
        String head = repository.deleteFile("config/ns-1.yaml", "remove first namespace");

        scheduler.sync();

        // the construct itself is retained in the database; only the VCS management
        // flag is cleared - the user deletes it through the REST API afterwards
        assertThat(nameSpaceRepository.findById("vcs-ns-1")).isPresent();
        assertThat(nameSpaceRepository.findById("vcs-ns-1").orElseThrow().isVcsManaged()).isFalse();
        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.SUCCESS);

        // once un-managed, the REST API accepts the deletion
        mockMvc.perform(delete("/v1_0/configs/nameSpaces/vcs-ns-1"))
                .andExpect(status().isAccepted());
        assertThat(nameSpaceRepository.findById("vcs-ns-1")).isEmpty();
    }

    @Test
    @Order(5)
    void invalidCommitIsRolledBackAndRecordedAsFailed() {
        Map<String, String> commit = new LinkedHashMap<>();
        commit.put("config/vcs-ns-3.yaml", namespace("vcs-ns-3", "namespace of a broken commit"));
        commit.put("config/zz-broken.yaml", dataset("vcs-broken", "vcs-ns-3", "missing-ds"));
        String head = commitAll(commit, "add commit that violates a foreign key");

        scheduler.sync();

        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.FAILED);
        assertThat(logFor(head).orElseThrow().getErrorMessage()).isNotBlank();
        // the whole commit is rolled back, including the valid namespace of the same commit
        assertThat(nameSpaceRepository.findById("vcs-ns-3")).isEmpty();
        assertThat(dataSetRepository.findById("vcs-broken")).isEmpty();

        // a failed commit is never retried on its own
        scheduler.sync();
        assertThat(vcsSyncLogRepository.findAll().stream()
                .filter(log -> head.equals(log.getCommitId()) && log.getStatus() == VcsSyncStatus.FAILED)
                .count()).isEqualTo(1);

        // removing the offending files lets the next cycle succeed
        String cleanup = deleteFiles(List.of("config/vcs-ns-3.yaml", "config/zz-broken.yaml"),
                "remove broken files");
        cleanup(cleanup);
        assertThat(nameSpaceRepository.findById("vcs-ns-3")).isEmpty();
    }

    @Test
    @Order(6)
    void unknownKindCommitIsRecordedAsFailedAndSkipped() {
        String head = commit("config/unknown.yaml", "kind: NoSuchKind\nkeyName: anything\n");

        scheduler.sync();

        assertThat(logFor(head).orElseThrow().getStatus()).isEqualTo(VcsSyncStatus.FAILED);
        assertThat(nameSpaceRepository.findById("anything")).isEmpty();

        scheduler.sync();
        assertThat(vcsSyncLogRepository.findAll().stream()
                .filter(log -> head.equals(log.getCommitId()) && log.getStatus() == VcsSyncStatus.FAILED)
                .count()).isEqualTo(1);

        cleanup(repository.deleteFile("config/unknown.yaml", "remove unknown kind file"));
    }

    @Test
    @Order(7)
    void updateModifiesTheExistingConstruct() {
        assertThat(nameSpaceRepository.findById("vcs-ns-2")).isPresent();
        String head = commit("config/vcs-ns-2.yaml", namespace("vcs-ns-2", "updated description"));

        scheduler.sync();

        assertThat(nameSpaceRepository.findById("vcs-ns-2").orElseThrow()
                .getDescription()).isEqualTo("updated description");
        assertThat(nameSpaceRepository.findById("vcs-ns-2").orElseThrow().isVcsManaged()).isTrue();
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

    private String namespace(String key, String description) {
        return """
                kind: NameSpace
                keyName: %s
                description: %s
                """.formatted(key, description);
    }

    private String datasource() {
        return """
                kind: DataSource
                keyName: vcs-ds
                description: gitops test datasource
                databaseProtocol: postgresql
                dataSourceType: database
                service:
                  host: localhost
                  port: "5432"
                  urn: testdb
                """;
    }

    private String dataset(String key, String namespace, String dataSource) {
        return """
                kind: DataSet
                keyName: %s
                nameSpaceKeyName: %s
                dataSourceKeyName: %s
                databaseSchemaName: test_schema
                tableName: %s
                description: gitops test data set
                sources: {}
                """.formatted(key, namespace, dataSource, key);
    }

    private String datasetWithSource(String key, String namespace, String dataSource, String sourceKey) {
        return """
                kind: DataSet
                keyName: %s
                nameSpaceKeyName: %s
                dataSourceKeyName: %s
                databaseSchemaName: test_schema
                tableName: %s
                description: gitops test data set
                sources:
                  %s:
                    properties:
                      kind: initial
                """.formatted(key, namespace, dataSource, key, sourceKey);
    }
}
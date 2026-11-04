/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.ui.modeller.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yarina.domain.layout.DomainLayout;
import org.yarina.domain.settings.DomainSettings;
import org.yarina.ui.modeller.service.AdminWorkspaceService;
import org.yarina.ui.modeller.service.EditorService;
import org.yarina.ui.modeller.service.ReviewService;
import org.yarina.ui.modeller.service.SchemaService;
import org.yarina.ui.modeller.service.SyncLogService;
import org.yarina.ui.modeller.service.VcsService;
import org.yarina.ui.modeller.service.YamlEditorService;
import org.yarina.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.yarina.ui.modeller.storage.WorkspaceStorage;
import org.yarina.ui.modeller.storage.s3.S3ObjectStorageClient;
import org.yarina.ui.modeller.storage.s3.S3WorkspaceStorage;
import org.yarina.ui.modeller.workspace.WorkspaceCleanupTask;
import org.yarina.ui.modeller.workspace.WorkspaceManager;
import org.yarina.ui.modeller.workspace.WorkspaceSeeder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Explicit wiring of the application-level strategies, all driven by the
 * {@code yarina.modeller.*} switchboard.
 */
@Configuration
public class ModellerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ModellerConfiguration.class);

    // ------------------------------------------------------------------
    // storage
    // ------------------------------------------------------------------

    @Bean
    public WorkspaceStorage workspaceStorage(ModellerProperties properties) {
        String type = properties.getStorage().getType();
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "s3", "minio" -> {
                ModellerProperties.S3 s3 = properties.getStorage().getS3();
                logger.info("Workspace storage: S3/MinIO bucket '{}' at {}", s3.getBucket(), s3.getEndpoint());
                S3ObjectStorageClient client = new S3ObjectStorageClient(
                        s3.getEndpoint(), s3.getBucket(), s3.getAccessKey(), s3.getSecretKey(), s3.getRegion());
                yield new S3WorkspaceStorage(client);
            }
            case "local", "filesystem", "" -> {
                LocalFsWorkspaceStorage storage =
                        new LocalFsWorkspaceStorage(properties.getStorage().getRootDirectory());
                logger.info("Workspace storage: local filesystem at {}", storage.root());
                yield storage;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported yarina.modeller.storage.type: " + type);
        };
    }

    // ------------------------------------------------------------------
    // VCS
    // ------------------------------------------------------------------

    @Bean
    public ModellerVcsCatalog vcsCatalog(DomainSettings domainSettings) {
        return new ModellerVcsCatalog(domainSettings);
    }

    // ------------------------------------------------------------------
    // workspaces
    // ------------------------------------------------------------------

    @Bean
    public WorkspaceManager workspaceManager(WorkspaceStorage storage, ModellerVcsCatalog vcs,
                                             DomainSettings domainSettings, ModellerProperties properties) {
        int ttlHours = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1, properties.getStorage().getCleanupTtlHours()));
        WorkspaceManager manager = new WorkspaceManager(storage,
                workspaceSeeder(vcs, domainSettings), ttlHours);
        return manager;
    }

    /**
     * Reads a branch and scopes it to the requested domain, logging the raw and the scoped
     * file counts. Without this an accidentally over-eager scoping is indistinguishable
     * from an empty repository: both leave the workspace folder empty.
     */
    private static WorkspaceSeeder workspaceSeeder(ModellerVcsCatalog vcs, DomainSettings domainSettings) {
        return (domain, branch) -> {
            long started = System.nanoTime();
            try {
                Map<String, String> raw = vcs.clientFor(domain).readBranchFiles(branch);
                boolean domainLayout = DomainLayout.usesDomainLayout(domain, raw, domainSettings.allDomainNames());
                Map<String, String> scoped = DomainLayout.scopeDomain(domain, raw,
                        domainSettings.allDomainNames());
                long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                if (!raw.isEmpty() && scoped.isEmpty()) {
                    logger.warn("Domain {} ({}): read {} file(s) from the repository but none remain after "
                                    + "domain scoping (domainLayout={}, knownDomains={}); first paths: {}",
                            domain, branch, raw.size(), domainLayout, domainSettings.allDomainNames(),
                            raw.keySet().stream().limit(10).toList());
                } else {
                    logger.info("Domain {} ({}): read {} file(s) from the repository, {} after domain scoping "
                                    + "(domainLayout={}) in {} ms",
                            domain, branch, raw.size(), scoped.size(), domainLayout, millis);
                }
                return scoped;
            } catch (RuntimeException e) {
                logger.error("Domain {} ({}): cannot read the branch from the VCS repository: {}",
                        domain, branch, e.getMessage(), e);
                throw e;
            }
        };
    }

    @Bean
    public WorkspaceCleanupTask workspaceCleanupTask(WorkspaceManager manager) {
        return new WorkspaceCleanupTask(manager);
    }

    // ------------------------------------------------------------------
    // services
    // ------------------------------------------------------------------

    @Bean
    public SyncLogService syncLogService(ModellerProperties properties) {
        return new SyncLogService(properties.getLogging().getSyncLogCapacity());
    }

    @Bean
    public YamlEditorService yamlEditorService() {
        return new YamlEditorService();
    }

    @Bean
    public SchemaService schemaService() {
        return new SchemaService();
    }

    @Bean
    public EditorService editorService(WorkspaceManager manager, WorkspaceStorage storage,
                                       YamlEditorService yaml, SyncLogService logs) {
        return new EditorService(manager, storage, yaml, logs);
    }

    @Bean
    public VcsService vcsService(WorkspaceManager manager, ModellerVcsCatalog vcs, WorkspaceStorage storage,
                                 SyncLogService logs, DomainSettings domainSettings) {
        return new VcsService(manager, vcs, storage, logs, domainSettings);
    }

    @Bean
    public ReviewService reviewService(WorkspaceManager manager, WorkspaceStorage storage, ModellerVcsCatalog vcs,
                                       DomainSettings domainSettings, SyncLogService logs) {
        return new ReviewService(manager, storage, vcs, domainSettings, logs);
    }

    @Bean
    public AdminWorkspaceService adminWorkspaceService(WorkspaceManager manager, SyncLogService logs) {
        return new AdminWorkspaceService(manager, logs);
    }
}
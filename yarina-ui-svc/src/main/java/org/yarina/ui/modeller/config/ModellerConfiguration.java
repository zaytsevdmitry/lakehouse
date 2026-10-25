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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

/**
 * Explicit wiring of the application-level strategies, all driven by the
 * {@code yarina.modeller.*} switchboard.
 */
@Configuration
public class ModellerConfiguration {

    // ------------------------------------------------------------------
    // storage
    // ------------------------------------------------------------------

    @Bean
    public WorkspaceStorage workspaceStorage(ModellerProperties properties) {
        String type = properties.getStorage().getType();
        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case "s3", "minio" -> {
                ModellerProperties.S3 s3 = properties.getStorage().getS3();
                S3ObjectStorageClient client = new S3ObjectStorageClient(
                        s3.getEndpoint(), s3.getBucket(), s3.getAccessKey(), s3.getSecretKey(), s3.getRegion());
                yield new S3WorkspaceStorage(client);
            }
            case "local", "filesystem", "" -> new LocalFsWorkspaceStorage(properties.getStorage().getRootDirectory());
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
                                             ModellerProperties properties) {
        int ttlHours = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1, properties.getStorage().getCleanupTtlHours()));
        WorkspaceManager manager = new WorkspaceManager(storage,
                (domain, branch) -> DomainLayout.scopeDomain(domain,
                        vcs.clientFor(domain).readBranchFiles(branch)),
                ttlHours);
        return manager;
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
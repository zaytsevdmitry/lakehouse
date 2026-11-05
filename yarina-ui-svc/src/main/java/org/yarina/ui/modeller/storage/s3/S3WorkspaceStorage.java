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

package org.yarina.ui.modeller.storage.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yarina.ui.modeller.storage.WorkspaceStorage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * S3/MinIO backed {@link WorkspaceStorage}: a workspace is a {@code workspaces/<id>/}
 * key prefix, each metadata file is an independent plain-text YAML object under that
 * prefix, streamed object-by-object.
 */
public class S3WorkspaceStorage implements WorkspaceStorage {

    private static final Logger logger = LoggerFactory.getLogger(S3WorkspaceStorage.class);

    private static final String WORKSPACES_PREFIX = "workspaces/";

    private final S3ObjectStorageClient client;

    public S3WorkspaceStorage(S3ObjectStorageClient client) {
        this.client = client;
    }

    @Override
    public boolean exists(String workspaceId) {
        return !listObjects(workspaceId).isEmpty();
    }

    @Override
    public void create(String workspaceId) {
        // S3 has no empty directories; existence is derived from objects under the prefix.
    }

    @Override
    public List<String> listFiles(String workspaceId) {
        List<String> files = new ArrayList<>();
        for (String key : listObjects(workspaceId))
            files.add(key.substring(prefix(workspaceId).length()));
        return files.stream().sorted().toList();
    }

    @Override
    public Optional<String> readFile(String workspaceId, String path) {
        byte[] bytes = client.getObject(objectKey(workspaceId, path));
        return bytes == null ? Optional.empty() : Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }

    @Override
    public void writeFile(String workspaceId, String path, String content) {
        String key = objectKey(workspaceId, path);
        byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
        try {
            client.putObject(key, bytes);
            logger.debug("Wrote object '{}' ({} byte(s)) of workspace {}", key, bytes.length, workspaceId);
        } catch (RuntimeException e) {
            logger.error("Cannot write object '{}' ({} byte(s)) of workspace {}: {}",
                    key, bytes.length, workspaceId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteFile(String workspaceId, String path) {
        client.deleteObject(objectKey(workspaceId, path));
    }

    @Override
    public Map<String, String> readAll(String workspaceId) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String path : listFiles(workspaceId)) {
            if (!path.endsWith(".yaml") && !path.endsWith(".yml"))
                continue;
            readFile(workspaceId, path).ifPresent(content -> result.put(path, content));
        }
        return result;
    }

    @Override
    public void writeAll(String workspaceId, Map<String, String> files) {
        long started = System.nanoTime();
        logger.info("Exporting {} object(s) of workspace {} to prefix '{}'", files.size(), workspaceId, prefix(workspaceId));
        int written = 0;
        try {
            for (String key : listObjects(workspaceId)) {
                logger.info("Workspace {}: removing previous object '{}' before export", workspaceId, key);
                client.deleteObject(key);
            }
            for (Map.Entry<String, String> entry : files.entrySet()) {
                logger.info("Workspace {}: writing object '{}' ({} byte(s))",
                        workspaceId, entry.getKey(),
                        entry.getValue() == null ? 0 : entry.getValue().getBytes(StandardCharsets.UTF_8).length);
                try {
                    writeFile(workspaceId, entry.getKey(), entry.getValue());
                    written++;
                } catch (RuntimeException e) {
                    logger.error("Workspace {}: failed to export object '{}' after {} of {} object(s): {}",
                            workspaceId, entry.getKey(), written, files.size(), e.getMessage(), e);
                    throw e;
                }
            }
            logger.info("Exported {} object(s) of workspace {} to prefix '{}' in {} ms",
                    written, workspaceId, prefix(workspaceId),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        } catch (RuntimeException e) {
            logger.error("Cannot replace content of workspace {} under prefix '{}' after {} of {} object(s): {}",
                    workspaceId, prefix(workspaceId), written, files.size(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteWorkspace(String workspaceId) {
        for (String key : listObjects(workspaceId))
            client.deleteObject(key);
    }

    @Override
    public List<String> listWorkspaces() {
        List<String> ids = new ArrayList<>(new LinkedHashSet<>());
        for (String key : client.listObjects(WORKSPACES_PREFIX)) {
            String rest = key.substring(WORKSPACES_PREFIX.length());
            int slash = rest.indexOf('/');
            String id = slash >= 0 ? rest.substring(0, slash) : rest;
            if (!id.isBlank())
                ids.add(id);
        }
        return ids;
    }

    private List<String> listObjects(String workspaceId) {
        return client.listObjects(prefix(workspaceId));
    }

    private String prefix(String workspaceId) {
        return WORKSPACES_PREFIX + workspaceId + "/";
    }

    private String objectKey(String workspaceId, String path) {
        return prefix(workspaceId) + path;
    }
}
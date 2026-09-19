package org.lakehouse.ui.modeller.storage.s3;

import org.lakehouse.ui.modeller.storage.WorkspaceStorage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * S3/MinIO backed {@link WorkspaceStorage}: a workspace is a {@code workspaces/<id>/}
 * key prefix, each metadata file is an independent plain-text YAML object under that
 * prefix, streamed object-by-object.
 */
public class S3WorkspaceStorage implements WorkspaceStorage {

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
        client.putObject(objectKey(workspaceId, path), content.getBytes(StandardCharsets.UTF_8));
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
        for (String key : listObjects(workspaceId))
            client.deleteObject(key);
        files.forEach((path, content) -> writeFile(workspaceId, path, content));
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
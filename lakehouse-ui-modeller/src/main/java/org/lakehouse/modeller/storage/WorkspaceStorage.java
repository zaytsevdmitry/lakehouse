package org.lakehouse.modeller.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstraction over the server-side workspace storage. Workspaces store plain-text YAML
 * metadata files independently, without binary Git archives.
 * <p>
 * Two interchangeable backends exist, selected via {@code lakehouse.configurator.storage.type}:
 * the local POSIX filesystem ({@link LocalFsWorkspaceStorage}) and S3/MinIO object storage
 * ({@link org.lakehouse.modeller.storage.s3.S3WorkspaceStorage}, streaming, no Git archives
 * held in RAM).
 */
public interface WorkspaceStorage {

    boolean exists(String workspaceId);

    void create(String workspaceId);

    List<String> listFiles(String workspaceId);

    Optional<String> readFile(String workspaceId, String path);

    void writeFile(String workspaceId, String path, String content);

    void deleteFile(String workspaceId, String path);

    /**
     * All metadata files of the workspace as a path-to-content map.
     */
    Map<String, String> readAll(String workspaceId);

    /**
     * Atomically replaces the whole file set (used when seeding a workspace from a VCS snapshot).
     */
    void writeAll(String workspaceId, Map<String, String> files);

    void deleteWorkspace(String workspaceId);

    /**
     * All workspace identifiers currently present in the backend.
     */
    List<String> listWorkspaces();
}
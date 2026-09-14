package org.lakehouse.modeller.storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstraction over the server-side workspace storage. Workspaces store plain-text YAML
 * metadata files independently, without binary Git archives.
 * <p>
 * Two interchangeable backends exist, selected via {@code lakehouse.modeller.storage.type}:
 * the local POSIX filesystem ({@link LocalFsWorkspaceStorage}) and S3/MinIO object storage
 * ({@link org.lakehouse.modeller.storage.s3.S3WorkspaceStorage}, streaming, no Git archives
 * held in RAM).
 */
public interface WorkspaceStorage {

    boolean exists(String workspaceId);

    void create(String workspaceId);

    List<String> listFiles(String workspaceId);

    /**
     * All directory paths of the workspace (including empty user-created folders),
     * each without a leading or trailing slash, sorted lexicographically.
     * Backends without a notion of empty directories (e.g. S3) return the
     * directories implied by the stored file paths.
     */
    default List<String> listDirectories(String workspaceId) {
        List<String> dirs = new java.util.ArrayList<>();
        for (String path : listFiles(workspaceId)) {
            int slash = path.lastIndexOf('/');
            while (slash > 0) {
                String dir = path.substring(0, slash);
                if (!dirs.contains(dir))
                    dirs.add(dir);
                slash = path.lastIndexOf('/', slash - 1);
            }
        }
        return dirs.stream().sorted().toList();
    }

    /**
     * Creates a directory. No-op for backends that cannot persist empty
     * directories (S3-style object storage); the folder then only appears
     * once a file is placed into it.
     */
    default void createDirectory(String workspaceId, String path) {
        // nothing to do by default
    }

    /**
     * Removes a directory and every file stored under it.
     */
    default void deleteDirectory(String workspaceId, String path) {
        String prefix = path.isEmpty() ? "" : path + "/";
        for (String filePath : listFiles(workspaceId)) {
            if (filePath.startsWith(prefix))
                deleteFile(workspaceId, filePath);
        }
    }

    /**
     * Moves a directory and the whole tree under it into {@code targetDirectory}
     * (empty means the workspace root). The source must be an existing directory
     * and must not be moved into its own subtree. Backends that cannot move whole
     * directories atomically copy every contained file and drop the source after
     * the copy.
     */
    default void moveDirectory(String workspaceId, String source, String targetDirectory) {
        String name = source.substring(source.lastIndexOf('/') + 1);
        String prefix = source + "/";
        String basePath = targetDirectory.isEmpty() ? name : targetDirectory + "/" + name;
        for (String filePath : listFiles(workspaceId)) {
            if (!filePath.startsWith(prefix))
                continue;
            String newPath = basePath + "/" + filePath.substring(prefix.length());
            readFile(workspaceId, filePath).ifPresent(content -> writeFile(workspaceId, newPath, content));
            deleteFile(workspaceId, filePath);
        }
        deleteDirectory(workspaceId, source);
    }

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
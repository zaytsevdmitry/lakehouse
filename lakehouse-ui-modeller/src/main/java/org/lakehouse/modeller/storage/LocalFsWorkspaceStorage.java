package org.lakehouse.modeller.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * POSIX filesystem {@link WorkspaceStorage}. Each workspace is a physical directory
 * {@code <root-directory>/workspaces/<workspaceId>/}; metadata files are plain YAML text.
 */
public class LocalFsWorkspaceStorage implements WorkspaceStorage {

    private final Path root;

    public LocalFsWorkspaceStorage(String rootDirectory) {
        this.root = Path.of(rootDirectory).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    @Override
    public boolean exists(String workspaceId) {
        return Files.isDirectory(dir(workspaceId));
    }

    @Override
    public void create(String workspaceId) {
        try {
            Files.createDirectories(dir(workspaceId));
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot create workspace " + workspaceId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> listFiles(String workspaceId) {
        List<Path> files = new ArrayList<>();
        Path wsDir = dir(workspaceId);
        try {
            collectFiles(wsDir, files);
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot list workspace " + workspaceId + ": " + e.getMessage(), e);
        }
        return files.stream()
                .map(p -> wsDir.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                .sorted()
                .toList();
    }

    private static void collectFiles(Path dir, List<Path> files) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry))
                    collectFiles(entry, files);
                else if (Files.isRegularFile(entry))
                    files.add(entry);
            }
        }
    }

    @Override
    public Optional<String> readFile(String workspaceId, String path) {
        Path file = safePath(workspaceId, path);
        if (!Files.isRegularFile(file))
            return Optional.empty();
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot read " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void writeFile(String workspaceId, String path, String content) {
        Path file = safePath(workspaceId, path);
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, content == null ? "" : content, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot write " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void deleteFile(String workspaceId, String path) {
        try {
            Files.deleteIfExists(safePath(workspaceId, path));
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot delete " + path + " of workspace " + workspaceId, e);
        }
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
        try {
            Path dir = dir(workspaceId);
            if (!Files.isDirectory(dir))
                Files.createDirectories(dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (!entry.getFileName().toString().equals("_workspace.json"))
                        deleteRecursively(entry);
                }
            }
            for (Map.Entry<String, String> entry : files.entrySet())
                writeFile(workspaceId, entry.getKey(), entry.getValue());
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot replace content of workspace " + workspaceId, e);
        }
    }

    @Override
    public void deleteWorkspace(String workspaceId) {
        try {
            Path dir = dir(workspaceId);
            if (!Files.isDirectory(dir))
                return;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream)
                    deleteRecursively(entry);
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot delete workspace " + workspaceId, e);
        }
    }

    private static void deleteRecursively(Path entry) throws IOException {
        if (Files.isDirectory(entry)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(entry)) {
                for (Path child : children)
                    deleteRecursively(child);
            }
        }
        Files.deleteIfExists(entry);
    }

    @Override
    public List<String> listDirectories(String workspaceId) {
        List<Path> dirs = new ArrayList<>();
        Path wsDir = dir(workspaceId);
        if (Files.isDirectory(wsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(wsDir)) {
                for (Path entry : stream)
                    collectDirectories(entry, dirs);
            } catch (IOException e) {
                throw new WorkspaceStorageException("Cannot list directories of workspace " + workspaceId, e);
            }
        }
        return dirs.stream()
                .map(p -> wsDir.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                .filter(p -> !p.isBlank())
                .sorted()
                .toList();
    }

    private static void collectDirectories(Path dir, List<Path> dirs) {
        if (!Files.isDirectory(dir))
            return;
        dirs.add(dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream)
                collectDirectories(entry, dirs);
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot list directories: " + e.getMessage(), e);
        }
    }

    @Override
    public void createDirectory(String workspaceId, String path) {
        try {
            Files.createDirectories(dir(workspaceId).resolve(path).normalize());
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot create directory " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void deleteDirectory(String workspaceId, String path) {
        Path target = dir(workspaceId).resolve(path).normalize();
        Path base = dir(workspaceId).normalize();
        if (!target.startsWith(base) || target.equals(base))
            throw new WorkspaceStorageException("Illegal directory path: " + path);
        try {
            if (Files.isDirectory(target))
                deleteRecursively(target);
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot delete directory " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public List<String> listWorkspaces() {
        List<String> ids = new ArrayList<>();
        Path workspacesDir = root.resolve("workspaces");
        if (!Files.isDirectory(workspacesDir))
            return ids;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspacesDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry))
                    ids.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            throw new WorkspaceStorageException("Cannot list workspaces: " + e.getMessage(), e);
        }
        return ids.stream().sorted().toList();
    }

    private Path dir(String workspaceId) {
        return root.resolve("workspaces").resolve(workspaceId).normalize();
    }

    /**
     * Prevents path traversal outside the workspace directory.
     */
    private Path safePath(String workspaceId, String path) {
        Path base = dir(workspaceId).normalize();
        Path target = base.resolve(path).normalize();
        if (!target.startsWith(base))
            throw new WorkspaceStorageException("Illegal workspace path: " + path);
        return target;
    }
}
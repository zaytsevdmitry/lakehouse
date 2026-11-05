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

package org.yarina.ui.modeller.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.concurrent.TimeUnit;

/**
 * POSIX filesystem {@link WorkspaceStorage}. Each workspace is a physical directory
 * {@code <root-directory>/workspaces/<workspaceId>/}; metadata files are plain YAML text.
 */
public class LocalFsWorkspaceStorage implements WorkspaceStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalFsWorkspaceStorage.class);

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
        Path dir = dir(workspaceId);
        try {
            Files.createDirectories(dir);
            logger.info("Created workspace directory {} for workspace {}", dir, workspaceId);
        } catch (IOException e) {
            logger.error("Cannot create workspace directory {} for workspace {}: {}",
                    dir, workspaceId, e.getMessage(), e);
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
            logger.error("Cannot list files of workspace {} in {}: {}", workspaceId, wsDir, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot list workspace " + workspaceId + ": " + e.getMessage(), e);
        }
        List<String> result = files.stream()
                .map(p -> wsDir.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                .sorted()
                .toList();
        logger.debug("Workspace {}: {} file(s) found in {}", workspaceId, result.size(), wsDir);
        return result;
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
            logger.error("Cannot read file '{}' of workspace {} from {}: {}",
                    path, workspaceId, file, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot read " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void writeFile(String workspaceId, String path, String content) {
        Path file = safePath(workspaceId, path);
        String value = content == null ? "" : content;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, value, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            logger.debug("Wrote file '{}' ({} byte(s)) of workspace {} to {}",
                    path, value.getBytes(StandardCharsets.UTF_8).length, workspaceId, file);
        } catch (IOException e) {
            logger.error("Cannot write file '{}' ({} byte(s)) of workspace {} to {}: {}",
                    path, value.getBytes(StandardCharsets.UTF_8).length, workspaceId, file, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot write " + path + " of workspace " + workspaceId, e);
        } catch (RuntimeException e) {
            logger.error("Unexpected failure writing file '{}' of workspace {} to {}: {}",
                    path, workspaceId, file, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteFile(String workspaceId, String path) {
        Path file = safePath(workspaceId, path);
        try {
            boolean deleted = Files.deleteIfExists(file);
            logger.debug("{} file '{}' of workspace {} at {}",
                    deleted ? "Deleted" : "File not found, nothing to delete for",
                    path, workspaceId, file);
        } catch (IOException e) {
            logger.error("Cannot delete file '{}' of workspace {} at {}: {}",
                    path, workspaceId, file, e.getMessage(), e);
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
        long started = System.nanoTime();
        Path dir = dir(workspaceId);
        long totalBytes = files.entrySet().stream()
                .mapToLong(entry -> entry.getValue() == null ? 0 : entry.getValue().getBytes(StandardCharsets.UTF_8).length)
                .sum();
        logger.info("Exporting {} file(s) ({} byte(s)) of workspace {} to {}",
                files.size(), totalBytes, workspaceId, dir);
        int written = 0;
        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
                logger.info("Created workspace directory {} for workspace {}", dir, workspaceId);
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    if (!entry.getFileName().toString().equals("_workspace.json")) {
                        logger.info("Workspace {}: removing previous content {} before export", workspaceId, entry);
                        deleteRecursively(entry);
                    }
                }
            }
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String path = entry.getKey();
                String content = entry.getValue();
                Path target = safePath(workspaceId, path);
                logger.info("Workspace {}: writing '{}' ({} byte(s)) to {}",
                        workspaceId, path, content == null ? 0 : content.getBytes(StandardCharsets.UTF_8).length, target);
                try {
                    writeFile(workspaceId, path, content);
                    written++;
                } catch (RuntimeException e) {
                    logger.error("Workspace {}: failed to export file '{}' to {} after {} of {} file(s): {}",
                            workspaceId, path, target, written, files.size(), e.getMessage(), e);
                    throw e;
                }
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            logger.info("Exported {} file(s) ({} byte(s)) of workspace {} to {} in {} ms",
                    written, totalBytes, workspaceId, dir, millis);
            verifyExport(workspaceId, files.size());
        } catch (IOException e) {
            logger.error("Cannot replace content of workspace {} at {} after {} of {} file(s): {}",
                    workspaceId, dir, written, files.size(), e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot replace content of workspace " + workspaceId, e);
        } catch (RuntimeException e) {
            logger.error("Cannot replace content of workspace {} at {} after {} of {} file(s): {}",
                    workspaceId, dir, written, files.size(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Reads the exported file set back, so a backend that silently loses files is visible in
     * the log instead of only in the workspace tree.
     */
    private void verifyExport(String workspaceId, int expected) {
        try {
            List<String> actual = listFiles(workspaceId);
            long exported = actual.stream()
                    .filter(path -> !path.equals("_workspace.json"))
                    .count();
            if (exported != expected) {
                logger.warn("Workspace {}: export verification failed, expected {} file(s) but found {} in {}; files: {}",
                        workspaceId, expected, exported, dir(workspaceId), actual);
            } else {
                logger.info("Workspace {}: export verified, {} file(s) are present in {}",
                        workspaceId, exported, dir(workspaceId));
            }
        } catch (RuntimeException e) {
            logger.error("Workspace {}: cannot verify the exported files in {}: {}",
                    workspaceId, dir(workspaceId), e.getMessage(), e);
        }
    }

    @Override
    public void deleteWorkspace(String workspaceId) {
        Path dir = dir(workspaceId);
        try {
            if (!Files.isDirectory(dir)) {
                logger.debug("Workspace {} has no directory at {}, nothing to delete", workspaceId, dir);
                return;
            }
            logger.info("Deleting workspace {} at {}", workspaceId, dir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream)
                    deleteRecursively(entry);
            }
            Files.deleteIfExists(dir);
            logger.info("Deleted workspace {} at {}", workspaceId, dir);
        } catch (IOException e) {
            logger.error("Cannot delete workspace {} at {}: {}", workspaceId, dir, e.getMessage(), e);
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
                logger.error("Cannot list directories of workspace {} in {}: {}",
                        workspaceId, wsDir, e.getMessage(), e);
                throw new WorkspaceStorageException("Cannot list directories of workspace " + workspaceId, e);
            }
        }
        List<String> result = dirs.stream()
                .map(p -> wsDir.relativize(p).toString().replace(java.io.File.separatorChar, '/'))
                .filter(p -> !p.isBlank())
                .sorted()
                .toList();
        logger.debug("Workspace {}: {} directorie(s) found in {}", workspaceId, result.size(), wsDir);
        return result;
    }

    private static void collectDirectories(Path dir, List<Path> dirs) {
        if (!Files.isDirectory(dir))
            return;
        dirs.add(dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream)
                collectDirectories(entry, dirs);
        } catch (IOException e) {
            logger.error("Cannot list directory {}: {}", dir, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot list directories: " + e.getMessage(), e);
        }
    }

    @Override
    public void createDirectory(String workspaceId, String path) {
        Path target = dir(workspaceId).resolve(path).normalize();
        try {
            Files.createDirectories(target);
            logger.debug("Created directory '{}' of workspace {} at {}", path, workspaceId, target);
        } catch (IOException e) {
            logger.error("Cannot create directory '{}' of workspace {} at {}: {}",
                    path, workspaceId, target, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot create directory " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void deleteDirectory(String workspaceId, String path) {
        Path target = dir(workspaceId).resolve(path).normalize();
        Path base = dir(workspaceId).normalize();
        if (!target.startsWith(base) || target.equals(base)) {
            logger.error("Illegal directory path '{}' of workspace {} (resolved to {})", path, workspaceId, target);
            throw new WorkspaceStorageException("Illegal directory path: " + path);
        }
        try {
            if (Files.isDirectory(target)) {
                logger.info("Deleting directory '{}' of workspace {} at {}", path, workspaceId, target);
                deleteRecursively(target);
            }
        } catch (IOException e) {
            logger.error("Cannot delete directory '{}' of workspace {} at {}: {}",
                    path, workspaceId, target, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot delete directory " + path + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public void moveDirectory(String workspaceId, String source, String targetDirectory) {
        Path base = dir(workspaceId).normalize();
        Path src = base.resolve(source).normalize();
        if (!src.startsWith(base))
            throw new WorkspaceStorageException("Illegal directory path: " + source);
        if (!Files.isDirectory(src))
            throw new WorkspaceStorageException("Directory not found: " + source);
        String name = source.substring(source.lastIndexOf('/') + 1);
        Path target = (targetDirectory.isEmpty() ? base : base.resolve(targetDirectory)).resolve(name).normalize();
        if (!target.startsWith(base))
            throw new WorkspaceStorageException("Illegal directory path: " + targetDirectory);
        if (target.equals(src) || target.startsWith(src))
            throw new WorkspaceStorageException("Cannot move a directory into itself: " + source);
        try {
            Files.createDirectories(target.getParent());
            Files.move(src, target);
            logger.info("Moved directory '{}' of workspace {} to {}", source, workspaceId, target);
        } catch (IOException e) {
            logger.error("Cannot move directory '{}' of workspace {} from {} to {}: {}",
                    source, workspaceId, src, target, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot move directory " + source + " of workspace " + workspaceId, e);
        }
    }

    @Override
    public List<String> listWorkspaces() {
        List<String> ids = new ArrayList<>();
        Path workspacesDir = root.resolve("workspaces");
        if (!Files.isDirectory(workspacesDir)) {
            logger.debug("Workspaces directory {} does not exist, no workspace found", workspacesDir);
            return ids;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(workspacesDir)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry))
                    ids.add(entry.getFileName().toString());
            }
        } catch (IOException e) {
            logger.error("Cannot list workspaces in {}: {}", workspacesDir, e.getMessage(), e);
            throw new WorkspaceStorageException("Cannot list workspaces: " + e.getMessage(), e);
        }
        List<String> result = ids.stream().sorted().toList();
        logger.debug("Found {} workspace(s) in {}", result.size(), workspacesDir);
        return result;
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
        if (!target.startsWith(base)) {
            logger.error("Illegal path '{}' of workspace {}: {} escapes {}", path, workspaceId, target, base);
            throw new WorkspaceStorageException("Illegal workspace path: " + path);
        }
        return target;
    }
}
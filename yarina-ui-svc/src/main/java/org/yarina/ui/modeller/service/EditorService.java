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

package org.yarina.ui.modeller.service;

import org.yarina.client.api.constant.YamlMetadataKind;
import org.yarina.ui.modeller.auth.DomainRole;
import org.yarina.ui.modeller.auth.ForbiddenException;
import org.yarina.ui.modeller.auth.NotFoundException;
import org.yarina.ui.modeller.auth.UserContext;
import org.yarina.ui.modeller.dto.CreateFileRequest;
import org.yarina.ui.modeller.dto.DirectoryRequest;
import org.yarina.ui.modeller.dto.FileContentResponse;
import org.yarina.ui.modeller.dto.MoveDirectoryRequest;
import org.yarina.ui.modeller.dto.MoveFileRequest;
import org.yarina.ui.modeller.dto.RenameFileRequest;
import org.yarina.ui.modeller.dto.SaveFileRequest;
import org.yarina.ui.modeller.dto.TreeResponse;
import org.yarina.ui.modeller.storage.WorkspaceStorage;
import org.yarina.ui.modeller.workspace.BranchSelection;
import org.yarina.ui.modeller.workspace.Workspace;
import org.yarina.ui.modeller.workspace.WorkspaceManager;
import org.yarina.ui.modeller.yaml.VcsConfigParseException;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * File-level CRUD over the server-side workspaces. Every mutating operation re-validates
 * the file path (no traversal, never {@code _workspace.json}) and the
 * parent workspace ownership; viewers are blocked from mutations. The repository layout
 * is arbitrary: metadata documents are recognized by their {@code kind} field, not by
 * the directory they live in, and any other text file is edited as a plain file.
 */
public class EditorService {

    private static final String UNKNOWN_KIND = "unknown";

    private final WorkspaceManager manager;
    private final WorkspaceStorage storage;
    private final YamlEditorService yaml;
    private final SyncLogService logs;

    public EditorService(WorkspaceManager manager, WorkspaceStorage storage,
                         YamlEditorService yaml, SyncLogService logs) {
        this.manager = manager;
        this.storage = storage;
        this.yaml = yaml;
        this.logs = logs;
    }

    public List<TreeResponse> tree(String workspaceId, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        List<TreeResponse> tree = new ArrayList<>();
        for (String path : storage.listFiles(workspaceId)) {
            if (path.endsWith("_workspace.json"))
                continue;
            // The repository layout and its file types are arbitrary: only YAML
            // metadata documents are parsed for their kind, everything else is
            // still listed and opened as plain text.
            if (!isYaml(path)) {
                tree.add(new TreeResponse(path, UNKNOWN_KIND, path));
                continue;
            }
            Optional<String> content;
            try {
                content = storage.readFile(workspaceId, path);
            } catch (RuntimeException e) {
                // an unreadable/binary file must not break the whole tree
                tree.add(new TreeResponse(path, UNKNOWN_KIND, path));
                continue;
            }
            content.ifPresent(text -> {
                try {
                    ObjectNode node = yaml.parse(text);
                    YamlMetadataKind kind = yaml.knownKindOf(node).orElse(null);
                    String kindValue = kind == null ? UNKNOWN_KIND : kind.yamlValue();
                    String identifierField = kind == null ? "keyName" : kind.identifierField();
                    String identifier = yaml.identifierOf(node, identifierField);
                    tree.add(new TreeResponse(path, kindValue, identifier == null ? path : identifier));
                } catch (VcsConfigParseException e) {
                    tree.add(new TreeResponse(path, UNKNOWN_KIND, path));
                }
            });
        }
        return tree.stream().sorted(java.util.Comparator.comparing(TreeResponse::path)).toList();
    }

    /**
     * Recursively resolves the upstream data sets of {@code keyName} (depth-first,
     * pre-order), returning every discovered source document exactly once in the
     * order the {@code sources} declarations are walked.
     */
    public List<FileContentResponse> lineageSources(String workspaceId, String keyName,
                                                    Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String targetKey = requireKeyName(keyName);
        Map<String, DatasetDocument> index = dataSetIndex(workspaceId);
        DatasetDocument target = index.get(targetKey);
        if (target == null)
            throw new NotFoundException("Data set not found: " + targetKey);
        requireViewer(user, domainOf(workspace, target.path()));

        Set<String> visited = new LinkedHashSet<>();
        visited.add(targetKey);
        List<FileContentResponse> result = new ArrayList<>();
        collectSources(targetKey, index, visited, result, workspace, user);
        return result;
    }

    private void collectSources(String keyName, Map<String, DatasetDocument> index,
                                Set<String> visited, List<FileContentResponse> result,
                                Workspace workspace, UserContext user) {
        DatasetDocument document = index.get(keyName);
        if (document == null)
            return;
        for (String sourceKey : sourceKeys(document.doc())) {
            if (!visited.add(sourceKey))
                continue;
            DatasetDocument source = index.get(sourceKey);
            if (source == null)
                continue;
            if (!canView(workspace, source.path(), user))
                continue;
            result.add(toResponse(source));
            collectSources(sourceKey, index, visited, result, workspace, user);
        }
    }

    /**
     * Recursively resolves the downstream data sets that consume {@code keyName}
     * (breadth-first): first the direct consumers, then their consumers, etc.
     * Every discovered receiver is returned exactly once.
     */
    public List<FileContentResponse> lineageReceivers(String workspaceId, String keyName,
                                                      Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String targetKey = requireKeyName(keyName);
        Map<String, DatasetDocument> index = dataSetIndex(workspaceId);
        DatasetDocument target = index.get(targetKey);
        if (target == null)
            throw new NotFoundException("Data set not found: " + targetKey);
        requireViewer(user, domainOf(workspace, target.path()));

        Map<String, List<String>> reverse = new LinkedHashMap<>();
        for (DatasetDocument document : index.values()) {
            for (String sourceKey : sourceKeys(document.doc()))
                reverse.computeIfAbsent(sourceKey, ignored -> new ArrayList<>()).add(document.keyName());
        }

        Set<String> visited = new LinkedHashSet<>();
        visited.add(targetKey);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(targetKey);
        List<FileContentResponse> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String key = queue.removeFirst();
            for (String receiverKey : reverse.getOrDefault(key, List.of())) {
                if (!visited.add(receiverKey))
                    continue;
                DatasetDocument receiver = index.get(receiverKey);
                if (receiver == null)
                    continue;
                if (canView(workspace, receiver.path(), user))
                    result.add(toResponse(receiver));
                queue.addLast(receiverKey);
            }
        }
        return result;
    }

    private Map<String, DatasetDocument> dataSetIndex(String workspaceId) {
        Map<String, DatasetDocument> index = new LinkedHashMap<>();
        for (String path : storage.listFiles(workspaceId)) {
            if (path.endsWith("_workspace.json"))
                continue;
            if (!path.endsWith(".yaml") && !path.endsWith(".yml"))
                continue;
            Optional<String> content = storage.readFile(workspaceId, path);
            if (content.isEmpty())
                continue;
            try {
                ObjectNode node = yaml.parse(content.get());
                if (yaml.knownKindOf(node).orElse(null) != YamlMetadataKind.DATA_SET)
                    continue;
                String key = yaml.identifierOf(node, YamlMetadataKind.DATA_SET.identifierField());
                if (key == null || key.isBlank())
                    continue;
                index.putIfAbsent(key, new DatasetDocument(path, key, node, content.get()));
            } catch (VcsConfigParseException e) {
                // an unreadable YAML file cannot participate in the lineage walk
            }
        }
        return index;
    }

    private static List<String> sourceKeys(ObjectNode document) {
        JsonNode sources = document.get("sources");
        if (sources == null || !sources.isObject())
            return List.of();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, JsonNode> entry : sources.properties()) {
            String key = entry.getKey();
            if (key != null && !key.isBlank())
                keys.add(key);
        }
        return keys;
    }

    private static FileContentResponse toResponse(DatasetDocument document) {
        return new FileContentResponse(document.path(), document.yaml(), YamlMetadataKind.DATA_SET.yamlValue(),
                document.keyName(), false);
    }

    public FileContentResponse createFile(String workspaceId, CreateFileRequest request,
                                          Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        if (request.kind() == null || request.keyName() == null || request.keyName().isBlank())
            throw new IllegalArgumentException("kind and keyName are required to create a file");
        YamlMetadataKind kind = YamlMetadataKind.fromYamlValue(request.kind());
        String fileName = safeFileName(request.keyName());
        // The repository layout is arbitrary: a new file goes exactly where the
        // caller asks for it (the selected folder), never into a directory
        // derived from the document kind. Without an explicit directory the file
        // is placed at the root of the first selected domain folder.
        String dir = safeDirectory(request.directory());
        if (dir.isEmpty() && !workspace.selections().isEmpty())
            dir = workspace.selections().get(0).folder();
        String path = dir.isEmpty() ? fileName + ".yaml" : dir + "/" + fileName + ".yaml";
        requireSelectionPath(workspace, path);
        requireEditor(user, domainOf(workspace, path));
        String content = yaml.defaultYaml(kind, request.keyName());
        storage.writeFile(workspaceId, path, content);
        logs.log("INFO", user.username(), "CREATE_FILE", path + " (" + kind.yamlValue() + ")", workspaceId);
        return new FileContentResponse(path, content, kind.yamlValue(), request.keyName(), true);
    }

    public FileContentResponse readFile(String workspaceId, String path, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String safePath = requireSelectionPath(workspace, safeFilePath(path));
        String content = storage.readFile(workspaceId, safePath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));
        return contentResponse(safePath, content);
    }

    public FileContentResponse saveFile(String workspaceId, String path, SaveFileRequest request,
                                        Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String safePath = requireSelectionPath(workspace, safeFilePath(path));
        requireEditor(user, selectionForPath(workspace, safePath).domain());
        if (!isYaml(safePath)) {
            String raw = request.yaml() == null ? "" : request.yaml();
            storage.writeFile(workspaceId, safePath, raw);
            logs.log("INFO", user.username(), "SAVE_FILE", safePath, workspaceId);
            return new FileContentResponse(safePath, raw, UNKNOWN_KIND, null, false);
        }
        ObjectNode node = yaml.parse(request.yaml());
        YamlMetadataKind kind = yaml.knownKindOf(node).orElse(null);
        String identifierField = kind == null ? "keyName" : kind.identifierField();
        String identifier = yaml.identifierOf(node, identifierField);
        if (identifier == null || identifier.isBlank())
            identifier = request.keyName();
        node = ensureIdentifier(node, identifierField, identifier);
        String content = yaml.serialize(node);
        storage.writeFile(workspaceId, safePath, content);
        logs.log("INFO", user.username(), "SAVE_FILE", safePath, workspaceId);
        String kindValue = kind == null ? UNKNOWN_KIND : kind.yamlValue();
        return new FileContentResponse(safePath, content, kindValue, identifier, keyEditable(kind));
    }

    public FileContentResponse renameFile(String workspaceId, RenameFileRequest request,
                                          Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String safePath = requireSelectionPath(workspace, safeFilePath(request.path()));
        requireEditor(user, selectionForPath(workspace, safePath).domain());
        String name = request.newName() == null ? "" : request.newName().trim();
        String originalName = safePath.substring(safePath.lastIndexOf('/') + 1);
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')) : ".yaml";
        // Keep the file type of an arbitrary file when only its base name changes;
        // metadata documents keep the historic .yaml suffix.
        if (!name.contains(".") || name.startsWith("."))
            name = name + extension;
        if (!name.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Invalid file name: " + request.newName());
        String parent = safePath.contains("/") ? safePath.substring(0, safePath.lastIndexOf('/')) : "";
        String newPath = (parent.isEmpty() ? "" : parent + "/") + name;
        requireSelectionPath(workspace, newPath);
        if (newPath.equals(safePath))
            throw new IllegalArgumentException("New name is identical to the current file name");
        String content = storage.readFile(workspaceId, safePath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + safePath));
        if (storage.readFile(workspaceId, newPath).isPresent())
            throw new IllegalArgumentException("Target file already exists: " + newPath);
        storage.writeFile(workspaceId, newPath, content);
        storage.deleteFile(workspaceId, safePath);
        logs.log("INFO", user.username(), "RENAME_FILE", safePath + " -> " + newPath, workspaceId);
        return contentResponse(newPath, content);
    }

    public void deleteFile(String workspaceId, String path, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String safePath = requireSelectionPath(workspace, safeFilePath(path));
        requireEditor(user, selectionForPath(workspace, safePath).domain());
        storage.deleteFile(workspaceId, safePath);
        logs.log("INFO", user.username(), "DELETE_FILE", safePath, workspaceId);
    }

    public FileContentResponse moveFile(String workspaceId, MoveFileRequest request,
                                        Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String source = requireSelectionPath(workspace, safeFilePath(request.source()));
        BranchSelection sourceSelection = selectionForPath(workspace, source);
        requireEditor(user, sourceSelection.domain());
        String targetDir = safeDirectory(request.targetDirectory());
        if (targetDir.isEmpty())
            targetDir = sourceSelection.folder();
        else
            requireSelectionPath(workspace, targetDir);
        String fileName = source.substring(source.lastIndexOf('/') + 1);
        String newPath = targetDir.isEmpty() ? fileName : targetDir + "/" + fileName;
        if (newPath.equals(source))
            throw new IllegalArgumentException("File is already in the target directory");
        if (storage.readFile(workspaceId, newPath).isPresent())
            throw new IllegalArgumentException("File already exists in the target directory: " + newPath);
        String content = storage.readFile(workspaceId, source)
                .orElseThrow(() -> new NotFoundException("File not found: " + source));
        storage.writeFile(workspaceId, newPath, content);
        storage.deleteFile(workspaceId, source);
        logs.log("INFO", user.username(), "MOVE_FILE", source + " -> " + newPath, workspaceId);
        return contentResponse(newPath, content);
    }

    public List<String> listDirectories(String workspaceId, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        requireOwnWorkspace(workspaceId, user);
        return storage.listDirectories(workspaceId);
    }

    public void moveDirectory(String workspaceId, MoveDirectoryRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String source = safeDirectory(request.source());
        if (source.isEmpty())
            throw new IllegalArgumentException("Cannot move the workspace root");
        requireSelectionPath(workspace, source);
        BranchSelection sourceSelection = selectionForPath(workspace, source);
        requireEditor(user, sourceSelection.domain());
        String targetDir = safeDirectory(request.targetDirectory());
        if (targetDir.isEmpty())
            targetDir = sourceSelection.folder();
        else
            requireSelectionPath(workspace, targetDir);
        String name = source.substring(source.lastIndexOf('/') + 1);
        String target = targetDir.isEmpty() ? name : targetDir + "/" + name;
        if (target.equals(source))
            throw new IllegalArgumentException("Directory is already in the target directory");
        if (target.startsWith(source + "/"))
            throw new IllegalArgumentException("Cannot move a directory into its own subtree");
        if (!storage.listDirectories(workspaceId).contains(source))
            throw new NotFoundException("Directory not found: " + source);
        if (storage.listDirectories(workspaceId).contains(target))
            throw new IllegalArgumentException("A folder with that name already exists in the target directory: " + target);
        storage.moveDirectory(workspaceId, source, targetDir);
        logs.log("INFO", user.username(), "MOVE_DIR", source + " -> " + target, workspaceId);
    }

    public void createDirectory(String workspaceId, DirectoryRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String dir = safeDirectory(request.path());
        if (dir.isEmpty()) {
            if (workspace.selections().isEmpty())
                throw new IllegalArgumentException("Workspace has no selected domain folders");
            dir = workspace.selections().get(0).folder();
        }
        requireSelectionPath(workspace, dir);
        requireEditor(user, domainOf(workspace, dir));
        storage.createDirectory(workspaceId, dir);
        logs.log("INFO", user.username(), "CREATE_DIR", dir, workspaceId);
    }

    public void deleteDirectory(String workspaceId, String path, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        String dir = safeDirectory(path);
        if (dir.isEmpty())
            throw new IllegalArgumentException("Cannot delete the workspace root");
        requireSelectionPath(workspace, dir);
        requireEditor(user, domainOf(workspace, dir));
        storage.deleteDirectory(workspaceId, dir);
        logs.log("INFO", user.username(), "DELETE_DIR", dir, workspaceId);
    }

    // ------------------------------------------------------------------
    // path / ownership guards
    // ------------------------------------------------------------------

    private static String requireSelectionPath(Workspace workspace, String path) {
        if (selectionForPath(workspace, path) == null)
            throw new IllegalArgumentException("Path must be inside a selected domain folder: " + path);
        return path;
    }

    private static BranchSelection selectionForPath(Workspace workspace, String path) {
        for (BranchSelection selection : workspace.selections())
            if (selection.covers(path))
                return selection;
        return null;
    }

    /** The identifier (key name) may be edited inline in the form for code scripts. */
    private static boolean keyEditable(YamlMetadataKind kind) {
        return kind == YamlMetadataKind.SCRIPT;
    }

    private Workspace requireOwnWorkspace(String workspaceId, UserContext user) {
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()))
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        return workspace;
    }

    /**
     * The domain that owns the given folder/path of a workspace — the domain of the
     * selection covering it, or the first selection when the path is not scoped.
     */
    private static String domainOf(Workspace workspace, String path) {
        BranchSelection selection = selectionForPath(workspace, path);
        if (selection != null)
            return selection.domain();
        if (!workspace.selections().isEmpty())
            return workspace.selections().get(0).domain();
        throw new IllegalArgumentException("Workspace has no selected domain folders");
    }

    private static UserContext requireEditor(UserContext user, String domain) {
        if (!user.hasDomainRole(domain, DomainRole.EDITOR))
            throw new ForbiddenException("Access denied: EDITOR role required for domain '" + domain + "'");
        return user;
    }

    /** Read-only lineage queries accept any viewer-level role of the owning domain. */
    private static UserContext requireViewer(UserContext user, String domain) {
        if (!user.hasDomainRole(domain, DomainRole.VIEWER, DomainRole.EDITOR, DomainRole.CONTROL))
            throw new ForbiddenException("Access denied: VIEWER role required for domain '" + domain + "'");
        return user;
    }

    private static boolean canView(Workspace workspace, String path, UserContext user) {
        try {
            requireViewer(user, domainOf(workspace, path));
            return true;
        } catch (ForbiddenException e) {
            return false;
        }
    }

    private static String requireKeyName(String keyName) {
        if (keyName == null || keyName.isBlank())
            throw new IllegalArgumentException("keyName must not be blank");
        return keyName;
    }

    private static String safeFilePath(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("File path must not be blank");
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.contains("..") || normalized.contains("\\"))
            throw new IllegalArgumentException("Illegal file path: " + path);
        if (normalized.endsWith("_workspace.json"))
            throw new IllegalArgumentException("_workspace.json is a system file");
        if (normalized.endsWith(".tmp"))
            throw new IllegalArgumentException("Temporary files are not editable: " + path);
        return normalized;
    }

    private static boolean isYaml(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }

    /** Builds the response for a file: YAML metadata documents by their kind, all other files as plain text. */
    private FileContentResponse contentResponse(String path, String content) {
        if (!isYaml(path))
            return new FileContentResponse(path, content, UNKNOWN_KIND, null, false);
        try {
            ObjectNode node = yaml.parse(content);
            YamlMetadataKind kind = yaml.knownKindOf(node).orElse(null);
            String kindValue = kind == null ? UNKNOWN_KIND : kind.yamlValue();
            String identifierField = kind == null ? "keyName" : kind.identifierField();
            return new FileContentResponse(path, content, kindValue,
                    yaml.identifierOf(node, identifierField), keyEditable(kind));
        } catch (VcsConfigParseException e) {
            return new FileContentResponse(path, content, UNKNOWN_KIND, null, false);
        }
    }

    private static String safeFileName(String keyName) {
        if (!keyName.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Invalid key name: " + keyName);
        return keyName.toLowerCase(Locale.ROOT);
    }

    /**
     * Validates a relative directory path (possibly empty, meaning the workspace root).
     * Rejects traversal and illegal segments; returns the path without slashes on the edges.
     */
    private static String safeDirectory(String path) {
        if (path == null || path.isBlank())
            return "";
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.endsWith("/"))
            normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.contains("..") || normalized.contains("\\"))
            throw new IllegalArgumentException("Illegal directory path: " + path);
        for (String segment : normalized.split("/")) {
            if (!segment.matches("[A-Za-z0-9._() -]+"))
                throw new IllegalArgumentException("Illegal directory name: " + path);
        }
        return normalized;
    }

    private static ObjectNode ensureIdentifier(ObjectNode node, String field, String value) {
        node.put(field, value);
        return node;
    }

    private record DatasetDocument(String path, String keyName, ObjectNode doc, String yaml) {
    }
}
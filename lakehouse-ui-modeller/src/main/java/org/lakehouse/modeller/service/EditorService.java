package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.ForbiddenException;
import org.lakehouse.modeller.auth.NotFoundException;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.auth.UserContextService;
import org.lakehouse.modeller.dto.CreateFileRequest;
import org.lakehouse.modeller.dto.DirectoryRequest;
import org.lakehouse.modeller.dto.FileContentResponse;
import org.lakehouse.modeller.dto.MoveDirectoryRequest;
import org.lakehouse.modeller.dto.MoveFileRequest;
import org.lakehouse.modeller.dto.RenameFileRequest;
import org.lakehouse.modeller.dto.SaveFileRequest;
import org.lakehouse.modeller.dto.TreeResponse;
import org.lakehouse.modeller.storage.WorkspaceStorage;
import org.lakehouse.modeller.workspace.Workspace;
import org.lakehouse.modeller.workspace.WorkspaceManager;
import org.lakehouse.modeller.yaml.ConfigKind;
import org.lakehouse.modeller.yaml.VcsConfigParseException;
import org.springframework.security.core.Authentication;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * File-level CRUD over the server-side workspaces. Every mutating operation re-validates
 * the file path (no traversal, YAML extension, never {@code _workspace.json}) and the
 * parent workspace ownership; viewers are blocked from mutations.
 */
public class EditorService {

    private final WorkspaceManager manager;
    private final WorkspaceStorage storage;
    private final UserContextService users;
    private final YamlEditorService yaml;
    private final SyncLogService logs;

    public EditorService(WorkspaceManager manager, WorkspaceStorage storage, UserContextService users,
                         YamlEditorService yaml, SyncLogService logs) {
        this.manager = manager;
        this.storage = storage;
        this.users = users;
        this.yaml = yaml;
        this.logs = logs;
    }

    public List<TreeResponse> tree(String workspaceId, Authentication authentication) {
        UserContext user = users.requireRole(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        List<TreeResponse> tree = new ArrayList<>();
        for (String path : storage.listFiles(workspaceId)) {
            if (path.endsWith("_workspace.json"))
                continue;
            storage.readFile(workspaceId, path).ifPresent(content -> {
                try {
                    ObjectNode node = yaml.parse(content);
                    ConfigKind kind = yaml.knownKindOf(node).orElse(null);
                    String kindValue = kind == null ? "unknown" : kind.yamlValue();
                    String identifierField = kind == null ? "keyName" : kind.identifierField();
                    String identifier = yaml.identifierOf(node, identifierField);
                    tree.add(new TreeResponse(path, kindValue, identifier == null ? path : identifier));
                } catch (VcsConfigParseException e) {
                    tree.add(new TreeResponse(path, "unknown", path));
                }
            });
        }
        return tree.stream().sorted(java.util.Comparator.comparing(TreeResponse::path)).toList();
    }

    public FileContentResponse createFile(String workspaceId, CreateFileRequest request,
                                          Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        if (request.kind() == null || request.keyName() == null || request.keyName().isBlank())
            throw new IllegalArgumentException("kind and keyName are required to create a file");
        ConfigKind kind = ConfigKind.fromYamlValue(request.kind());
        String fileName = safeFileName(request.keyName());
        String dir = safeDirectory(request.directory());
        String path = (dir.isEmpty() ? kind.directory() : dir) + "/" + fileName + ".yaml";
        String content = yaml.defaultYaml(kind, request.keyName());
        storage.writeFile(workspaceId, path, content);
        logs.log("INFO", user.username(), "CREATE_FILE", path + " (" + kind.yamlValue() + ")", workspaceId);
        return new FileContentResponse(path, content, kind.yamlValue(), request.keyName(), true);
    }

    public FileContentResponse readFile(String workspaceId, String path, Authentication authentication) {
        UserContext user = users.requireRole(authentication);
        requireOwnWorkspace(workspaceId, user);
        String safePath = safeFilePath(path);
        String content = storage.readFile(workspaceId, safePath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));
        try {
            ObjectNode node = yaml.parse(content);
            ConfigKind kind = yaml.knownKindOf(node).orElse(null);
            String kindValue = kind == null ? "unknown" : kind.yamlValue();
            String identifierField = kind == null ? "keyName" : kind.identifierField();
            return new FileContentResponse(safePath, content, kindValue,
                    yaml.identifierOf(node, identifierField), keyEditable(kind));
        } catch (VcsConfigParseException e) {
            return new FileContentResponse(safePath, content, "unknown", null, false);
        }
    }

    public FileContentResponse saveFile(String workspaceId, String path, SaveFileRequest request,
                                        Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String safePath = safeFilePath(path);
        ObjectNode node = yaml.parse(request.yaml());
        ConfigKind kind = yaml.knownKindOf(node).orElse(null);
        String identifierField = kind == null ? "keyName" : kind.identifierField();
        String identifier = yaml.identifierOf(node, identifierField);
        if (identifier == null || identifier.isBlank())
            identifier = request.keyName();
        node = ensureIdentifier(node, identifierField, identifier);
        String content = yaml.serialize(node);
        storage.writeFile(workspaceId, safePath, content);
        logs.log("INFO", user.username(), "SAVE_FILE", safePath, workspaceId);
        String kindValue = kind == null ? "unknown" : kind.yamlValue();
        return new FileContentResponse(safePath, content, kindValue, identifier, keyEditable(kind));
    }

    public FileContentResponse renameFile(String workspaceId, RenameFileRequest request,
                                          Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String safePath = safeFilePath(request.path());
        String name = request.newName() == null ? "" : request.newName().trim();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".yaml"))
            name = name + ".yaml";
        if (!name.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Invalid file name: " + request.newName());
        String parent = safePath.contains("/") ? safePath.substring(0, safePath.lastIndexOf('/')) : "";
        String newPath = (parent.isEmpty() ? "" : parent + "/") + name;
        if (newPath.equals(safePath))
            throw new IllegalArgumentException("New name is identical to the current file name");
        String content = storage.readFile(workspaceId, safePath)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + safePath));
        if (storage.readFile(workspaceId, newPath).isPresent())
            throw new IllegalArgumentException("Target file already exists: " + newPath);
        storage.writeFile(workspaceId, newPath, content);
        storage.deleteFile(workspaceId, safePath);
        logs.log("INFO", user.username(), "RENAME_FILE", safePath + " -> " + newPath, workspaceId);
        ObjectNode node = yaml.parse(content);
        ConfigKind kind = yaml.knownKindOf(node).orElse(null);
        String kindValue = kind == null ? "unknown" : kind.yamlValue();
        String identifierField = kind == null ? "keyName" : kind.identifierField();
        return new FileContentResponse(newPath, content, kindValue, yaml.identifierOf(node, identifierField), keyEditable(kind));
    }

    public void deleteFile(String workspaceId, String path, Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String safePath = safeFilePath(path);
        storage.deleteFile(workspaceId, safePath);
        logs.log("INFO", user.username(), "DELETE_FILE", safePath, workspaceId);
    }

    public FileContentResponse moveFile(String workspaceId, MoveFileRequest request,
                                        Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String source = safeFilePath(request.source());
        String targetDir = safeDirectory(request.targetDirectory());
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
        ObjectNode node = yaml.parse(content);
        ConfigKind kind = yaml.knownKindOf(node).orElse(null);
        String kindValue = kind == null ? "unknown" : kind.yamlValue();
        String identifierField = kind == null ? "keyName" : kind.identifierField();
        return new FileContentResponse(newPath, content, kindValue, yaml.identifierOf(node, identifierField), keyEditable(kind));
    }

    public List<String> listDirectories(String workspaceId, Authentication authentication) {
        UserContext user = users.requireRole(authentication);
        requireOwnWorkspace(workspaceId, user);
        return storage.listDirectories(workspaceId);
    }

    public void moveDirectory(String workspaceId, MoveDirectoryRequest request, Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String source = safeDirectory(request.source());
        if (source.isEmpty())
            throw new IllegalArgumentException("Cannot move the workspace root");
        String targetDir = safeDirectory(request.targetDirectory());
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
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String dir = safeDirectory(request.path());
        if (dir.isEmpty())
            throw new IllegalArgumentException("Directory path must not be empty");
        storage.createDirectory(workspaceId, dir);
        logs.log("INFO", user.username(), "CREATE_DIR", dir, workspaceId);
    }

    public void deleteDirectory(String workspaceId, String path, Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        requireOwnWorkspace(workspaceId, user);
        String dir = safeDirectory(path);
        if (dir.isEmpty())
            throw new IllegalArgumentException("Cannot delete the workspace root");
        storage.deleteDirectory(workspaceId, dir);
        logs.log("INFO", user.username(), "DELETE_DIR", dir, workspaceId);
    }

    // ------------------------------------------------------------------
    // path / ownership guards
    // ------------------------------------------------------------------

    /** The identifier (key name) may be edited inline in the form for code scripts. */
    private static boolean keyEditable(ConfigKind kind) {
        return kind == ConfigKind.SCRIPT;
    }

    private Workspace requireOwnWorkspace(String workspaceId, UserContext user) {
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()) && user.effectiveRole() != org.lakehouse.modeller.auth.ModellerRole.ADMIN)
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        return workspace;
    }

    private static String safeFilePath(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("File path must not be blank");
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        if (normalized.contains("..") || normalized.contains("\\"))
            throw new IllegalArgumentException("Illegal file path: " + path);
        if (!normalized.endsWith(".yaml") && !normalized.endsWith(".yml"))
            throw new IllegalArgumentException("Only .yaml metadata files are editable: " + path);
        if (normalized.endsWith("_workspace.json"))
            throw new IllegalArgumentException("_workspace.json is a system file");
        return normalized;
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
            if (!segment.matches("[A-Za-z0-9._-]+"))
                throw new IllegalArgumentException("Illegal directory name: " + path);
        }
        return normalized;
    }

    private static ObjectNode ensureIdentifier(ObjectNode node, String field, String value) {
        node.put(field, value);
        return node;
    }
}
package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.ForbiddenException;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.auth.UserContextService;
import org.lakehouse.modeller.dto.CreateFileRequest;
import org.lakehouse.modeller.dto.FileContentResponse;
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
        String path = kind.directory() + "/" + fileName + ".yaml";
        String content = yaml.defaultYaml(kind, request.keyName());
        storage.writeFile(workspaceId, path, content);
        logs.log("INFO", user.username(), "CREATE_FILE", path + " (" + kind.yamlValue() + ")", workspaceId);
        return new FileContentResponse(path, content, kind.yamlValue(), request.keyName(), true);
    }

    public FileContentResponse readFile(String workspaceId, String path, Authentication authentication) {
        UserContext user = users.requireRole(authentication);
        requireOwnWorkspace(workspaceId, user);
        String safePath = safeFilePath(path);
        return storage.readFile(workspaceId, safePath)
                .map(content -> {
                    ObjectNode node = yaml.parse(content);
                    ConfigKind kind = yaml.knownKindOf(node).orElse(null);
                    String kindValue = kind == null ? "unknown" : kind.yamlValue();
                    String identifierField = kind == null ? "keyName" : kind.identifierField();
                    return new FileContentResponse(safePath, content, kindValue,
                            yaml.identifierOf(node, identifierField), keyEditable(kind));
                })
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + path));
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

    private static ObjectNode ensureIdentifier(ObjectNode node, String field, String value) {
        node.put(field, value);
        return node;
    }
}
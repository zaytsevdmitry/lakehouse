package org.lakehouse.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.dto.CreateFileRequest;
import org.lakehouse.ui.modeller.dto.DirectoryRequest;
import org.lakehouse.ui.modeller.dto.MoveDirectoryRequest;
import org.lakehouse.ui.modeller.dto.MoveFileRequest;
import org.lakehouse.ui.modeller.dto.RenameFileRequest;
import org.lakehouse.ui.modeller.dto.SaveFileRequest;
import org.lakehouse.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.lakehouse.ui.modeller.workspace.BranchSelection;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every editor operation must stay inside the generated selection folders
 * ({@code <domain> (<branch>)}) — the workspace root itself is not a valid target.
 */
class EditorServiceSelectionScopeTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @TempDir
    Path temp;

    private LocalFsWorkspaceStorage storage;

    private WorkspaceManager manager;

    private EditorService editor;

    private Workspace workspace;

    private UsernamePasswordAuthenticationToken authentication;

    private void givenWorkspaceWithTwoDomains() {
        storage = new LocalFsWorkspaceStorage(temp.toString());
        manager = new WorkspaceManager(storage, (domain, branch) -> switch (domain) {
            case "platform" -> Map.of("config/namespace/ns.yaml", "kind: NameSpace\nkeyName: ns\n");
            case "analytics" -> Map.of("config/dataset/ds.yaml", "kind: DataSet\nkeyName: ds\n");
            default -> Map.of();
        }, 4, Clock.fixed(T0, ZoneOffset.UTC));
        workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));
        editor = new EditorService(manager, storage, new YamlEditorService(), new SyncLogService(10));
        authentication = authentication("alice");
    }

    private static UsernamePasswordAuthenticationToken authentication(String username) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .build();
        return new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    }

    @Test
    void createFileRejectsADirectoryOutsideTheSelectionFolders() {
        givenWorkspaceWithTwoDomains();
        assertThatThrownBy(() -> editor.createFile(workspace.id(),
                new CreateFileRequest("DataSet", "orders", "config/dataset"), authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selected domain folder");
        assertThat(storage.listFiles(workspace.id())).doesNotContain("config/dataset/orders.yaml");
    }

    @Test
    void createDirectoryDefaultsToTheFirstSelectionFolder() {
        givenWorkspaceWithTwoDomains();
        editor.createDirectory(workspace.id(), new DirectoryRequest(""), authentication);
        assertThat(storage.listDirectories(workspace.id())).contains("platform (main)");
    }

    @Test
    void readSaveAndDeleteRejectPathsOutsideTheSelectionFolders() {
        givenWorkspaceWithTwoDomains();
        storage.writeFile(workspace.id(), "config/dataset/loose.yaml", "kind: DataSet\nkeyName: loose\n");

        assertThatThrownBy(() -> editor.readFile(workspace.id(), "config/dataset/loose.yaml", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("selected domain folder");
        assertThatThrownBy(() -> editor.saveFile(workspace.id(), "config/dataset/loose.yaml",
                new SaveFileRequest("config/dataset/loose.yaml", "kind: DataSet\n", "loose"), authentication))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> editor.deleteFile(workspace.id(), "config/dataset/loose.yaml", authentication))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.listFiles(workspace.id())).contains("config/dataset/loose.yaml");
    }

    @Test
    void renameAndMoveRejectPathsOutsideTheSelectionFolders() {
        givenWorkspaceWithTwoDomains();
        storage.writeFile(workspace.id(), "config/dataset/loose.yaml", "kind: DataSet\nkeyName: loose\n");

        assertThatThrownBy(() -> editor.renameFile(workspace.id(),
                new RenameFileRequest("config/dataset/loose.yaml", "renamed"), authentication))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> editor.moveFile(workspace.id(),
                new MoveFileRequest("config/dataset/loose.yaml", "config/dataset/nested"), authentication))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> editor.moveDirectory(workspace.id(),
                new MoveDirectoryRequest("config/dataset", "config/other"), authentication))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.listFiles(workspace.id())).contains("config/dataset/loose.yaml");
    }

    @Test
    void moveToAnEmptyTargetStaysInsideTheOwningDomainFolder() {
        givenWorkspaceWithTwoDomains();
        editor.moveFile(workspace.id(),
                new MoveFileRequest("analytics (dev)/config/dataset/ds.yaml", ""), authentication);
        assertThat(storage.listFiles(workspace.id()))
                .contains("analytics (dev)/ds.yaml")
                .doesNotContain("analytics (dev)/config/dataset/ds.yaml");
    }

    @Test
    void moveDirectoryToAnEmptyTargetStaysInsideTheOwningDomainFolder() {
        givenWorkspaceWithTwoDomains();
        editor.moveDirectory(workspace.id(),
                new MoveDirectoryRequest("analytics (dev)/config/dataset", ""), authentication);
        assertThat(storage.listFiles(workspace.id()))
                .contains("analytics (dev)/dataset/ds.yaml")
                .doesNotContain("analytics (dev)/config/dataset/ds.yaml");
    }
}

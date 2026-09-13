package org.lakehouse.modeller.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFsWorkspaceStorageTest {

    @TempDir
    Path temp;

    private LocalFsWorkspaceStorage storage() {
        return new LocalFsWorkspaceStorage(temp.toString());
    }

    @Test
    void createMakesWorkspaceVisibleAndListable() {
        LocalFsWorkspaceStorage storage = storage();
        assertThat(storage.exists("w1")).isFalse();
        storage.create("w1");
        assertThat(storage.exists("w1")).isTrue();
        assertThat(storage.listWorkspaces()).containsExactly("w1");
    }

    @Test
    void writeReadDeleteRoundTrip() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "config/namespace/ns.yaml", "kind: NameSpace\n");
        assertThat(storage.readFile("w1", "config/namespace/ns.yaml"))
                .isEqualTo(Optional.of("kind: NameSpace\n"));
        storage.deleteFile("w1", "config/namespace/ns.yaml");
        assertThat(storage.readFile("w1", "config/namespace/ns.yaml")).isEmpty();
    }

    @Test
    void writeAllAtomicallyReplacesContent() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeAll("w1", Map.of("a.yaml", "first"));
        storage.writeAll("w1", Map.of("b.yaml", "second", "c.yaml", "third"));
        assertThat(storage.listFiles("w1")).isEqualTo(java.util.List.of("b.yaml", "c.yaml"));
        assertThat(storage.readAll("w1"))
                .containsExactlyInAnyOrderEntriesOf(Map.of("b.yaml", "second", "c.yaml", "third"));
    }

    @Test
    void readAllSkipsNonYamlFiles() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "note.txt", "not yaml");
        storage.writeFile("w1", "seed.yaml", "kind: NameSpace\n");
        assertThat(storage.readAll("w1")).containsOnlyKeys("seed.yaml");
    }

    @Test
    void deleteWorkspaceRemovesWholeTree() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "config/namespace/ns.yaml", "kind: NameSpace\n");
        storage.deleteWorkspace("w1");
        assertThat(storage.exists("w1")).isFalse();
        assertThat(storage.listWorkspaces()).isEmpty();
    }

    @Test
    void pathTraversalOutsideWorkspaceIsRejected() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        assertThatThrownBy(() -> storage.writeFile("w1", "../../evil.yaml", "boom"))
                .isInstanceOf(WorkspaceStorageException.class)
                .hasMessageContaining("Illegal workspace path");
        assertThatThrownBy(() -> storage.readFile("w1", "../other.yaml"))
                .isInstanceOf(WorkspaceStorageException.class);
    }

    @Test
    void moveDirectoryMovesTheWholeTreeAndKeepsContent() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "a/one.yaml", "kind: NameSpace\n");
        storage.writeFile("w1", "a/sub/two.yaml", "kind: NameSpace\n");
        storage.moveDirectory("w1", "a", "target/deep");
        assertThat(storage.listFiles("w1"))
                .containsExactly("target/deep/a/one.yaml", "target/deep/a/sub/two.yaml");
        assertThat(storage.readFile("w1", "target/deep/a/one.yaml"))
                .isEqualTo(Optional.of("kind: NameSpace\n"));
        assertThat(storage.listDirectories("w1")).doesNotContain("a", "a/sub");
    }

    @Test
    void moveDirectoryToRootMovesTheTreeUp() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "parent/a/one.yaml", "kind: NameSpace\n");
        storage.writeFile("w1", "sibling.yaml", "kind: NameSpace\n");
        storage.moveDirectory("w1", "parent/a", "");
        assertThat(storage.listFiles("w1"))
                .containsExactly("a/one.yaml", "sibling.yaml");
        assertThat(storage.listDirectories("w1"))
                .containsExactly("a", "parent");
    }

    @Test
    void moveDirectoryIntoItselfIsRejected() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "a/one.yaml", "kind: NameSpace\n");
        storage.writeFile("w1", "a/sub/two.yaml", "kind: NameSpace\n");
        assertThatThrownBy(() -> storage.moveDirectory("w1", "a", "a/sub"))
                .isInstanceOf(WorkspaceStorageException.class)
                .hasMessageContaining("Cannot move a directory into itself");
        assertThat(storage.listFiles("w1"))
                .containsExactly("a/one.yaml", "a/sub/two.yaml");
    }

    @Test
    void moveDirectoryOntoExistingTargetIsRejected() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        storage.writeFile("w1", "a/one.yaml", "kind: NameSpace\n");
        storage.writeFile("w1", "b/a/two.yaml", "kind: NameSpace\n");
        assertThatThrownBy(() -> storage.moveDirectory("w1", "a", "b"))
                .isInstanceOf(WorkspaceStorageException.class)
                .hasMessageContaining("Cannot move directory a of workspace w1");
        assertThat(storage.listFiles("w1"))
                .containsExactly("a/one.yaml", "b/a/two.yaml");
    }

    @Test
    void moveDirectoryMissingSourceIsRejected() {
        LocalFsWorkspaceStorage storage = storage();
        storage.create("w1");
        assertThatThrownBy(() -> storage.moveDirectory("w1", "missing", ""))
                .isInstanceOf(WorkspaceStorageException.class)
                .hasMessageContaining("Directory not found");
    }
}
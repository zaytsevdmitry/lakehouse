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
}
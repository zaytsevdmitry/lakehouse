/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.config.cvs.client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lakehouse.config.cvs.CvsChangeType;
import org.lakehouse.config.cvs.CvsClientException;
import org.lakehouse.config.cvs.CvsDiffEntry;
import org.lakehouse.config.cvs.TestGitRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitCvsClientTest {

    private static Path baseDir;
    private static TestGitRepository repository;

    @BeforeAll
    static void setUp() throws IOException {
        baseDir = Files.createTempDirectory("git-cvs-client-test");
        repository = TestGitRepository.create(baseDir);
    }

    @AfterAll
    static void tearDown() throws IOException {
        repository.close();
        try (var paths = Files.walk(baseDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException ignored) {
                            //best effort cleanup
                        }
                    });
        }
    }

    private GitCvsClient newClient() {
        return new GitCvsClient(repository.bareUri(), "main", repository.clonePath().toString(), null);
    }

    private GitCvsClient initAndPull() {
        GitCvsClient client = newClient();
        client.init();
        client.pull();
        return client;
    }

    @Test
    void clonesBranchAndReadsFiles() {
        GitCvsClient client = initAndPull();

        assertThat(client.getCurrentCommitId()).isEqualTo(repository.head());
        assertThat(client.readFileContent(client.getCurrentCommitId(), "README.md")).isPresent();
        assertThat(client.readFileContent(client.getCurrentCommitId(), "missing.yaml")).isEmpty();
    }

    @Test
    void initReopensAnExistingClone() {
        GitCvsClient first = initAndPull();
        assertThat(first.readFileContent(first.getCurrentCommitId(), "README.md")).isPresent();

        GitCvsClient reopened = initAndPull();
        assertThat(reopened.readFileContent(reopened.getCurrentCommitId(), "README.md")).isPresent();
    }

    @Test
    void diffWithoutBaseListsEveryFileAsCreated() {
        repository.commitFile("config/alpha.yaml", "kind: NameSpace\nkeyName: alpha\n", "add alpha");
        GitCvsClient client = initAndPull();

        List<CvsDiffEntry> diff = client.getDiff(null);
        assertThat(diff).extracting(CvsDiffEntry::path)
                .contains("README.md", "config/alpha.yaml");
        assertThat(diff).allMatch(entry -> entry.type() == CvsChangeType.CREATED);
    }

    @Test
    void diffReportsCreationUpdateAndDeletion() {
        String base = repository.head();
        String createdHead = repository.commitFile(
                "config/ns.yaml", "kind: NameSpace\nkeyName: ns\n", "add namespace");
        GitCvsClient client = initAndPull();

        assertThat(client.getDiff(base))
                .containsExactly(new CvsDiffEntry("config/ns.yaml", CvsChangeType.CREATED));

        String updatedHead = repository.commitFile(
                "config/ns.yaml", "kind: NameSpace\nkeyName: ns\ndescription: v2\n", "update namespace");
        client.pull();
        assertThat(client.getDiff(createdHead))
                .containsExactly(new CvsDiffEntry("config/ns.yaml", CvsChangeType.UPDATED));

        String deletedHead = repository.deleteFile("config/ns.yaml", "delete namespace");
        client.pull();
        assertThat(client.getDiff(updatedHead))
                .containsExactly(new CvsDiffEntry("config/ns.yaml", CvsChangeType.DELETED));

        assertThat(client.getDiff(base)).isEmpty();
    }

    @Test
    void diffTurnsRenameIntoDeleteAndCreate() {
        repository.commitFile("config/rename-me.yaml", "kind: NameSpace\nkeyName: renamed\n", "add file");
        String beforeRename = repository.head();
        repository.moveFile("config/rename-me.yaml", "config/after-rename.yaml", "rename file");
        GitCvsClient client = initAndPull();

        assertThat(client.getDiff(beforeRename))
                .containsExactlyInAnyOrder(
                        new CvsDiffEntry("config/rename-me.yaml", CvsChangeType.DELETED),
                        new CvsDiffEntry("config/after-rename.yaml", CvsChangeType.CREATED));
    }

    @Test
    void operationsBeforeInitAreRejected() {
        GitCvsClient client = newClient();
        assertThatThrownBy(client::pull).isInstanceOf(CvsClientException.class);
        assertThatThrownBy(client::getCurrentCommitId).isInstanceOf(CvsClientException.class);
        assertThatThrownBy(() -> client.getDiff(null)).isInstanceOf(CvsClientException.class);
    }
}
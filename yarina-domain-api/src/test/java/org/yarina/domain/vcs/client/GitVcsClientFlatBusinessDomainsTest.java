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

package org.yarina.domain.vcs.client;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yarina.domain.layout.DomainLayout;
import org.yarina.domain.settings.GitCredentialsSettings;
import org.yarina.domain.vcs.VcsReviewResult;
import org.yarina.domain.vcs.VcsReviewSubmission;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A per-domain repository that keeps a business folder named {@code domains/} must be
 * read and written as a flat repository: its files stay at the repository root.
 */
class GitVcsClientFlatBusinessDomainsTest {

    private static final String BUSINESS_FILE = "domains/aaaa/datasets/report.yaml";
    private static final String SCHEDULE_FILE = "schedules/initial.yaml";

    @TempDir
    Path temp;

    private File bareDir;

    @BeforeEach
    void seedFlatRepository() throws Exception {
        bareDir = temp.resolve("analytics.git").toFile();
        try (Git ignored = Git.init().setBare(true).setDirectory(bareDir).setInitialBranch("main").call()) {
        }
        File work = temp.resolve("seed-work").toFile();
        try (Git workGit = Git.init().setDirectory(work).setInitialBranch("main").call()) {
            write(workGit, BUSINESS_FILE, "kind: DataSet\nkeyName: report\n");
            write(workGit, SCHEDULE_FILE, "kind: Schedule\nkeyName: initial\n");
            write(workGit, "README.md", "# analytics");
            workGit.add().addFilepattern(".").call();
            workGit.commit().setMessage("initial")
                    .setAuthor(new PersonIdent("init", "init@yarina.local"))
                    .setCommitter(new PersonIdent("init", "init@yarina.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }
    }

    /**
     * Seeds a second repository whose business folders are called {@code domains/} and
     * which has no root-level file at all (the layout of the real flocktory repository).
     */
    private File seedBusinessDomainsOnlyRepository() throws Exception {
        File repoDir = temp.resolve("flocktory.git").toFile();
        try (Git ignored = Git.init().setBare(true).setDirectory(repoDir).setInitialBranch("main").call()) {
        }
        File work = temp.resolve("seed-work-flocktory").toFile();
        try (Git workGit = Git.init().setDirectory(work).setInitialBranch("main").call()) {
            write(workGit, "domains/ai/datasets/report.yaml", "kind: DataSet\nkeyName: ai__report\n");
            write(workGit, "domains/finance/datasets/ledger.yaml", "kind: DataSet\nkeyName: finance__ledger\n");
            write(workGit, "domains/platform/tasks/check.yaml", "kind: Task\nname: check\n");
            workGit.add().addFilepattern(".").call();
            workGit.commit().setMessage("initial")
                    .setAuthor(new PersonIdent("init", "init@yarina.local"))
                    .setCommitter(new PersonIdent("init", "init@yarina.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(repoDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }
        return repoDir;
    }

    private static GitVcsClient provider(File repoDir) {
        GitCredentialsSettings account = new GitCredentialsSettings();
        account.setAuthType("token");
        account.setToken("dummy");
        // "platform" is both a configured domain and a business folder of the flocktory
        // repository: a single matching name must not turn the flat repository into a container
        return new GitVcsClient(repoDir.getAbsolutePath(), "main", GitCredentials.forDomain(account),
                Set.of("flocktory", "analytics", "platform"));
    }

    private static void write(Git git, String path, String content) throws Exception {
        File target = new File(git.getRepository().getWorkTree(), path);
        Files.createDirectories(target.toPath().getParent());
        Files.writeString(target.toPath(), content);
    }

    private GitVcsClient provider() {
        GitCredentialsSettings account = new GitCredentialsSettings();
        account.setAuthType("token");
        account.setToken("dummy");
        return new GitVcsClient(bareDir.getAbsolutePath(), "main",
                GitCredentials.forDomain(account));
    }

    @Test
    void workspaceSnapshotContainsEveryFileOfTheFlatRepository() {
        GitVcsClient provider = provider();

        Map<String, String> scoped = DomainLayout.scopeDomain("analytics", provider.readBranchFiles("main"));

        assertThat(scoped).containsOnlyKeys(BUSINESS_FILE, SCHEDULE_FILE);
    }

    @Test
    void reviewKeepsFlatPathsAndNeverCreatesADomainSubtree() throws Exception {
        GitVcsClient provider = provider();
        provider.createBranch("feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission("analytics", "feature", "main", "Update the report", null,
                        Map.of(BUSINESS_FILE, "kind: DataSet\nkeyName: report\n",
                                SCHEDULE_FILE, "kind: Schedule\nkeyName: initial\n",
                                "datasets/new.yaml", "kind: DataSet\nkeyName: new\n")),
                "Alice", "alice@yarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branch = provider.readBranchFiles("feature");
        assertThat(branch)
                .containsEntry(BUSINESS_FILE, "kind: DataSet\nkeyName: report\n")
                .containsEntry(SCHEDULE_FILE, "kind: Schedule\nkeyName: initial\n")
                .containsEntry("datasets/new.yaml", "kind: DataSet\nkeyName: new\n")
                .doesNotContainKey("domains/analytics/datasets/new.yaml");
        assertThat(branch).doesNotContainKey("domains/analytics/" + BUSINESS_FILE);
    }

    @Test
    void repositoryWhoseEveryFileLivesUnderBusinessDomainsFoldersIsReadWhole() throws Exception {
        GitVcsClient provider = provider(seedBusinessDomainsOnlyRepository());

        Map<String, String> scoped = DomainLayout.scopeDomain("flocktory", provider.readBranchFiles("main"));

        assertThat(scoped).containsOnlyKeys(
                "domains/ai/datasets/report.yaml",
                "domains/finance/datasets/ledger.yaml",
                "domains/platform/tasks/check.yaml");
    }

    @Test
    void reviewOfABusinessDomainsOnlyRepositoryNeverCreatesADomainSubtree() throws Exception {
        GitVcsClient provider = provider(seedBusinessDomainsOnlyRepository());
        provider.createBranch("feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission("flocktory", "feature", "main", "Update the report", null,
                        Map.of("domains/ai/datasets/report.yaml", "kind: DataSet\nkeyName: ai__report\n",
                                "domains/ai/datasets/new.yaml", "kind: DataSet\nkeyName: ai__new\n")),
                "Alice", "alice@yarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branch = provider.readBranchFiles("feature");
        assertThat(branch)
                .containsEntry("domains/ai/datasets/report.yaml", "kind: DataSet\nkeyName: ai__report\n")
                .containsEntry("domains/ai/datasets/new.yaml", "kind: DataSet\nkeyName: ai__new\n")
                .doesNotContainKey("domains/flocktory/domains/ai/datasets/new.yaml")
                .doesNotContainKey("domains/flocktory/domains/ai/datasets/report.yaml");
    }
}

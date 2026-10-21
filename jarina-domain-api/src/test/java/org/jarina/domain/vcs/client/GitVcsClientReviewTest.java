/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.domain.vcs.client;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jarina.domain.vcs.VcsException;
import org.jarina.domain.vcs.VcsReviewResult;
import org.jarina.domain.vcs.VcsReviewSubmission;
import org.jarina.domain.settings.GitCredentialsSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Review-side operations of {@link GitVcsClient} against a transient central repository.
 */
class GitVcsClientReviewTest {

    private static final String INITIAL = "config/namespace/ns.yaml";

    private static final String DOMAIN = "default";

    @TempDir
    Path temp;

    private File bareDir;

    @BeforeEach
    void seedCentralRepository() throws Exception {
        bareDir = temp.resolve("central.git").toFile();
        try (Git ignored = Git.init().setBare(true).setDirectory(bareDir).setInitialBranch("main").call()) {
            // empty bare repo is the remote; HEAD points at refs/heads/main
        }

        File work = temp.resolve("seed-work").toFile();
        try (Git workGit = Git.init().setDirectory(work).setInitialBranch("main").call()) {
            File target = new File(work, INITIAL);
            Files.createDirectories(target.getParentFile().toPath());
            Files.writeString(target.toPath(), "kind: NameSpace\nkeyName: ns\n");
            workGit.add().addFilepattern(".").call();
            workGit.commit()
                    .setMessage("initial")
                    .setAuthor(new PersonIdent("init", "init@jarina.local"))
                    .setCommitter(new PersonIdent("init", "init@jarina.local"))
                    .call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(
                    new RefSpec("HEAD:refs/heads/main")).call();
        }
    }

    private GitVcsClient provider() {
        GitCredentialsSettings account = new GitCredentialsSettings();
        account.setAuthType("token");
        account.setToken("dummy");
        return new GitVcsClient(bareDir.getAbsolutePath(), "main",
                GitCredentials.forDomain(account));
    }

    @Test
    void readsBranchFilesOfTheCentralRepository() {
        GitVcsClient provider = provider();
        Map<String, String> files = provider.readBranchFiles("main");
        assertThat(files).containsKey(INITIAL);
        assertThat(files.get(INITIAL)).contains("kind: NameSpace");
    }

    @Test
    void listsBranchesFromTheCentralRepository() {
        GitVcsClient provider = provider();
        assertThat(provider.listBranches()).contains("main");
    }

    @Test
    void createsBranchesForkedFromMain() {
        GitVcsClient provider = provider();
        provider.createBranch("feature", "main");
        assertThat(provider.listBranches()).contains("feature", "main");
    }

    @Test
    void submitReviewCommitsWorkspaceWithUserAuthorAndTechnicalCommitter() throws Exception {
        GitVcsClient provider = provider();
        provider.createBranch("feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "feature", "main", "Add featured namespace", null,
                        Map.of("config/namespace/featured.yaml", "kind: NameSpace\nkeyName: featured\n")),
                "Alice", "alice@jarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branchFiles = provider.readBranchFiles("feature");
        assertThat(branchFiles).containsKey("config/namespace/featured.yaml");

        try (Repository repo = new FileRepositoryBuilder().setGitDir(bareDir).setMustExist(true).build()) {
            Ref ref = repo.findRef("refs/heads/feature");
            assertThat(ref).isNotNull();
            RevWalk walk = new RevWalk(repo);
            RevCommit head = walk.parseCommit(ref.getObjectId());
            walk.parseHeaders(head);
            PersonIdent author = head.getAuthorIdent();
            PersonIdent committer = head.getCommitterIdent();
            assertThat(author.getName()).isEqualTo("Alice");
            assertThat(author.getEmailAddress()).isEqualTo("alice@jarina.local");
            assertThat(committer.getName()).isEqualTo("jarina-modeller-svc");
            assertThat(committer.getEmailAddress()).isEqualTo("jarina-modeller-svc@jarina.local");
            walk.close();
        }
    }

    @Test
    void submitReviewToTheCloneDefaultBranchPushesAndMerges() throws Exception {
        GitVcsClient provider = provider();

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "main", "main", "Direct update on main", null,
                        Map.of(INITIAL, "kind: NameSpace\nkeyName: ns\n",
                                "config/namespace/shared.yaml", "kind: NameSpace\nkeyName: shared\n")),
                "Alice", "alice@jarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branchFiles = provider.readBranchFiles("main");
        assertThat(branchFiles).containsKey("config/namespace/shared.yaml");
        assertThat(branchFiles).containsKey(INITIAL);
    }

    @Test
    void submitReviewRemovesYamlFilesDroppedFromTheWorkspace() {
        GitVcsClient provider = provider();
        provider.createBranch("cleanup", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "cleanup", "main", "Drop the namespace", null,
                        Map.of("config/namespace/only.yaml", "kind: NameSpace\nkeyName: only\n")),
                "Alice", "alice@jarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        assertThat(provider.readBranchFiles("cleanup"))
                .containsOnlyKeys("config/namespace/only.yaml");
    }

    @Test
    void submitReviewKeepsNonYamlFiles() throws Exception {
        File work = temp.resolve("non-yaml-work").toFile();
        try (Git workGit = Git.init().setDirectory(work).setInitialBranch("main").call()) {
            Files.createDirectories(new File(work, "config/namespace").toPath());
            Files.writeString(new File(work, INITIAL).toPath(), "kind: NameSpace\nkeyName: ns\n");
            Files.writeString(new File(work, "README.md").toPath(), "# keep me");
            workGit.add().addFilepattern(".").call();
            workGit.commit().setMessage("initial")
                    .setAuthor(new PersonIdent("init", "init@jarina.local"))
                    .setCommitter(new PersonIdent("init", "init@jarina.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setForce(true).setRemote("origin")
                    .setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }

        GitVcsClient provider = provider();
        provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "main", "main", "Keep the readme", null,
                        Map.of("config/namespace/added.yaml", "kind: NameSpace\nkeyName: added\n")),
                "Alice", "alice@jarina.local");

        try (Repository repo = new FileRepositoryBuilder().setGitDir(bareDir).setMustExist(true).build()) {
            RevWalk walk = new RevWalk(repo);
            RevCommit head = walk.parseCommit(repo.findRef("refs/heads/main").getObjectId());
            org.eclipse.jgit.revwalk.RevTree tree = walk.parseTree(head.getTree());
            org.eclipse.jgit.treewalk.TreeWalk files = new org.eclipse.jgit.treewalk.TreeWalk(repo);
            files.addTree(tree);
            files.setRecursive(true);
            java.util.List<String> paths = new java.util.ArrayList<>();
            while (files.next())
                paths.add(files.getPathString());
            assertThat(paths).contains("README.md", "config/namespace/added.yaml");
            assertThat(paths).doesNotContain(INITIAL);
            files.close();
            walk.close();
        }
    }

    @Test
    void submitReviewToAMissingBranchIsRejected() {
        GitVcsClient provider = provider();
        assertThatThrownBy(() -> provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "no-such-branch", "main", "Nope", null,
                        Map.of("config/namespace/x.yaml", "kind: NameSpace\nkeyName: x\n")),
                "Alice", "alice@jarina.local"))
                .isInstanceOf(VcsException.class)
                .hasMessageContaining("no-such-branch");
    }
}

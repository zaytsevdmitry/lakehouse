package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalGitVcsProviderTest {

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
                    .setAuthor(new PersonIdent("init", "init@lakehouse.local"))
                    .setCommitter(new PersonIdent("init", "init@lakehouse.local"))
                    .call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(
                    new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/main")).call();
        }
    }

    private LocalGitVcsProvider provider(String vcsProviderHints) {
        ModellerProperties properties = new ModellerProperties();
        if (vcsProviderHints != null)
            properties.setVcsProvider(vcsProviderHints);
        properties.getGit().setRemoteUrl(bareDir.getAbsolutePath());
        properties.getGit().setBranchMain("main");
        properties.getVcsSystemAccount().setAuthType("token");
        properties.getVcsSystemAccount().setToken("dummy");
        CredentialsProvider credentials = GitCredentials.forSystemAccount(properties.getVcsSystemAccount());
        return new LocalGitVcsProvider(properties, credentials);
    }

    private UserContext user() {
        return new UserContext("alice", "Alice", "alice@lakehouse.local", Set.of("LAKEHOUSE_MODELLER_EDITOR"));
    }

    @Test
    void readsBranchFilesOfTheCentralRepository() {
        LocalGitVcsProvider provider = provider("local-git");
        Map<String, String> files = provider.readBranchFiles(DOMAIN, "main");
        assertThat(files).containsKey(INITIAL);
        assertThat(files.get(INITIAL)).contains("kind: NameSpace");
    }

    @Test
    void listsBranchesFromTheCentralRepository() {
        LocalGitVcsProvider provider = provider("local-git");
        assertThat(provider.listBranches(DOMAIN)).contains("main");
    }

    @Test
    void createsBranchesForkedFromMain() {
        LocalGitVcsProvider provider = provider("local-git");
        provider.createBranch(DOMAIN, "feature", "main");
        assertThat(provider.listBranches(DOMAIN)).contains("feature", "main");
    }

    @Test
    void submitReviewCommitsWorkspaceWithUserAuthorAndTechnicalCommitter() throws Exception {
        LocalGitVcsProvider provider = provider("local-git");
        provider.createBranch(DOMAIN, "feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "feature", "main", "Add featured namespace", null,
                        Map.of("config/namespace/featured.yaml", "kind: NameSpace\nkeyName: featured\n")),
                user());

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branchFiles = provider.readBranchFiles(DOMAIN, "feature");
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
            assertThat(author.getEmailAddress()).isEqualTo("alice@lakehouse.local");
            assertThat(committer.getName()).isEqualTo("lakehouse-modeller-svc");
            assertThat(committer.getEmailAddress()).isEqualTo("lakehouse-modeller-svc@lakehouse.local");
            walk.close();
        }
    }

    @Test
    void submitReviewToTheCloneDefaultBranchPushesAndMerges() throws Exception {
        LocalGitVcsProvider provider = provider("local-git");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "main", "main", "Direct update on main", null,
                        Map.of(INITIAL, "kind: NameSpace\nkeyName: ns\n",
                                "config/namespace/shared.yaml", "kind: NameSpace\nkeyName: shared\n")),
                user());

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branchFiles = provider.readBranchFiles(DOMAIN, "main");
        assertThat(branchFiles).containsKey("config/namespace/shared.yaml");
        assertThat(branchFiles).containsKey(INITIAL);
    }

    @Test
    void submitReviewRemovesYamlFilesDroppedFromTheWorkspace() {
        LocalGitVcsProvider provider = provider("local-git");
        provider.createBranch(DOMAIN, "cleanup", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "cleanup", "main", "Drop the namespace", null,
                        Map.of("config/namespace/only.yaml", "kind: NameSpace\nkeyName: only\n")),
                user());

        assertThat(result.status()).isEqualTo("PUSHED");
        assertThat(provider.readBranchFiles(DOMAIN, "cleanup"))
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
                    .setAuthor(new PersonIdent("init", "init@lakehouse.local"))
                    .setCommitter(new PersonIdent("init", "init@lakehouse.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setForce(true).setRemote("origin")
                    .setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }

        LocalGitVcsProvider provider = provider("local-git");
        provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "main", "main", "Keep the readme", null,
                        Map.of("config/namespace/added.yaml", "kind: NameSpace\nkeyName: added\n")),
                user());

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
        LocalGitVcsProvider provider = provider("local-git");
        assertThatThrownBy(() -> provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "no-such-branch", "main", "Nope", null,
                        Map.of("config/namespace/x.yaml", "kind: NameSpace\nkeyName: x\n")),
                user()))
                .isInstanceOf(VcsProviderException.class)
                .hasMessageContaining("no-such-branch");
    }

    @Test
    void gerritProviderPushesToMagicRefsForBranch() throws Exception {
        LocalGitVcsProvider provider = provider("gerrit");
        provider.createBranch(DOMAIN, "gerrit-branch", "main");
        provider.submitReview(
                new VcsReviewSubmission(DOMAIN, "gerrit-branch", "main", "Gerrit change", null,
                        Map.of("config/namespace/gerrit.yaml", "kind: NameSpace\nkeyName: gerrit\n")),
                user());

        try (Repository repo = new FileRepositoryBuilder().setGitDir(bareDir).setMustExist(true).build()) {
            Ref ref = repo.findRef("refs/for/gerrit-branch");
            assertThat(ref).isNotNull();
            ObjectId objectId = ref.getObjectId();
            assertThat(objectId).isNotNull();
        }
    }
}
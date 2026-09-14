package org.lakehouse.modeller.vcs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.config.ModellerProperties;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LocalGitVcsProviderTest {

    private static final String INITIAL = "config/namespace/ns.yaml";

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
        Map<String, String> files = provider.readBranchFiles("main");
        assertThat(files).containsKey(INITIAL);
        assertThat(files.get(INITIAL)).contains("kind: NameSpace");
    }

    @Test
    void listsBranchesFromTheCentralRepository() {
        LocalGitVcsProvider provider = provider("local-git");
        assertThat(provider.listBranches()).contains("main");
    }

    @Test
    void createsBranchesForkedFromMain() {
        LocalGitVcsProvider provider = provider("local-git");
        provider.createBranch("feature", "main");
        assertThat(provider.listBranches()).contains("feature", "main");
    }

    @Test
    void submitReviewCommitsWorkspaceWithUserAuthorAndTechnicalCommitter() throws Exception {
        LocalGitVcsProvider provider = provider("local-git");
        provider.createBranch("feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission("feature", "main", "Add featured namespace", null,
                        Map.of("config/namespace/featured.yaml", "kind: NameSpace\nkeyName: featured\n")),
                user());

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
                new VcsReviewSubmission("main", "main", "Direct update on main", null,
                        Map.of("config/namespace/shared.yaml", "kind: NameSpace\nkeyName: shared\n")),
                user());

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> branchFiles = provider.readBranchFiles("main");
        assertThat(branchFiles).containsKey("config/namespace/shared.yaml");
        assertThat(branchFiles).containsKey(INITIAL);
    }

    @Test
    void gerritProviderPushesToMagicRefsForBranch() throws Exception {
        LocalGitVcsProvider provider = provider("gerrit");
        provider.createBranch("gerrit-branch", "main");
        provider.submitReview(
                new VcsReviewSubmission("gerrit-branch", "main", "Gerrit change", null,
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
package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One repository hosting several domains under {@code domains/<domain>/}. A review of a
 * single domain must leave the sibling domain subtrees and every non-YAML file untouched.
 */
class LocalGitCentralLayoutTest {

    private static final String PLATFORM_FILE = "domains/platform/config/namespace/platform.yaml";
    private static final String ANALYTICS_FILE = "domains/analytics/config/dataset/report.yaml";

    @TempDir
    Path temp;

    private File bareDir;

    @BeforeEach
    void seedCentralRepository() throws Exception {
        bareDir = temp.resolve("central.git").toFile();
        try (Git ignored = Git.init().setBare(true).setDirectory(bareDir).setInitialBranch("main").call()) {
        }
        File work = temp.resolve("seed-work").toFile();
        try (Git workGit = Git.init().setDirectory(work).setInitialBranch("main").call()) {
            write(workGit, PLATFORM_FILE, "kind: NameSpace\nkeyName: platform\n");
            write(workGit, ANALYTICS_FILE, "kind: DataSet\nkeyName: report\n");
            write(workGit, "README.md", "# central repository");
            workGit.add().addFilepattern(".").call();
            workGit.commit().setMessage("initial")
                    .setAuthor(new PersonIdent("init", "init@lakehouse.local"))
                    .setCommitter(new PersonIdent("init", "init@lakehouse.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }
    }

    private static void write(Git git, String path, String content) throws Exception {
        File target = new File(git.getRepository().getWorkTree(), path);
        Files.createDirectories(target.toPath().getParent());
        Files.writeString(target.toPath(), content);
    }

    private LocalGitVcsProvider provider() {
        ModellerProperties properties = new ModellerProperties();
        properties.setVcsProvider("local-git");
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
    void scopesTheRepositoryToTheRequestedDomain() {
        LocalGitVcsProvider provider = provider();
        assertThat(WorkspaceManager.scopeDomain("analytics", provider.readBranchFiles("analytics", "main")))
                .containsOnlyKeys("config/dataset/report.yaml");
        assertThat(WorkspaceManager.scopeDomain("platform", provider.readBranchFiles("platform", "main")))
                .containsOnlyKeys("config/namespace/platform.yaml");
    }

    @Test
    void reviewOfOneDomainKeepsTheOtherDomainAndNonYamlFiles() throws Exception {
        LocalGitVcsProvider provider = provider();
        provider.createBranch("analytics", "feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission("analytics", "feature", "main", "Add a dataset", null,
                        Map.of("config/dataset/report.yaml", "kind: DataSet\nkeyName: report\n",
                                "config/dataset/extra.yaml", "kind: DataSet\nkeyName: extra\n")),
                user());

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> analytics =
                WorkspaceManager.scopeDomain("analytics", provider.readBranchFiles("analytics", "feature"));
        assertThat(analytics).containsOnlyKeys("config/dataset/report.yaml", "config/dataset/extra.yaml");
        assertThat(WorkspaceManager.scopeDomain("platform", provider.readBranchFiles("platform", "main")))
                .containsOnlyKeys("config/namespace/platform.yaml");
        assertThat(provider.readBranchFiles("analytics", "feature")).containsKey(PLATFORM_FILE);
    }

    @Test
    void droppingADomainFileDoesNotTouchSiblingDomains() {
        LocalGitVcsProvider provider = provider();
        provider.createBranch("analytics", "trim", "main");

        provider.submitReview(
                new VcsReviewSubmission("analytics", "trim", "main", "Drop the report", null,
                        Map.of("config/dataset/extra.yaml", "kind: DataSet\nkeyName: extra\n")),
                user());

        Map<String, String> branch = provider.readBranchFiles("analytics", "trim");
        assertThat(branch)
                .containsKey("domains/analytics/config/dataset/extra.yaml")
                .doesNotContainKey(ANALYTICS_FILE)
                .containsKey(PLATFORM_FILE);
    }
}

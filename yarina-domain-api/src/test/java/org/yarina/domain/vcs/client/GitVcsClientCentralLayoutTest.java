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
import org.yarina.domain.vcs.VcsReviewResult;
import org.yarina.domain.vcs.VcsReviewSubmission;
import org.yarina.domain.settings.GitCredentialsSettings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One repository hosting several domains under {@code domains/<domain>/}. A review of a
 * single domain must leave the sibling domain subtrees and every non-YAML file untouched.
 */
class GitVcsClientCentralLayoutTest {

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
                    .setAuthor(new PersonIdent("init", "init@yarina.local"))
                    .setCommitter(new PersonIdent("init", "init@yarina.local")).call();
            workGit.remoteAdd().setName("origin").setUri(new URIish(bareDir.getAbsolutePath())).call();
            workGit.push().setRemote("origin").setRefSpecs(new RefSpec("HEAD:refs/heads/main")).call();
        }
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
    void scopesTheRepositoryToTheRequestedDomain() {
        GitVcsClient provider = provider();
        assertThat(DomainLayout.scopeDomain("analytics", provider.readBranchFiles("main")))
                .containsOnlyKeys("config/dataset/report.yaml");
        assertThat(DomainLayout.scopeDomain("platform", provider.readBranchFiles("main")))
                .containsOnlyKeys("config/namespace/platform.yaml");
    }

    @Test
    void reviewOfOneDomainKeepsTheOtherDomainAndNonYamlFiles() throws Exception {
        GitVcsClient provider = provider();
        provider.createBranch("feature", "main");

        VcsReviewResult result = provider.submitReview(
                new VcsReviewSubmission("analytics", "feature", "main", "Add a dataset", null,
                        Map.of("config/dataset/report.yaml", "kind: DataSet\nkeyName: report\n",
                                "config/dataset/extra.yaml", "kind: DataSet\nkeyName: extra\n")),
                "Alice", "alice@yarina.local");

        assertThat(result.status()).isEqualTo("PUSHED");
        Map<String, String> analytics =
                DomainLayout.scopeDomain("analytics", provider.readBranchFiles("feature"));
        assertThat(analytics).containsOnlyKeys("config/dataset/report.yaml", "config/dataset/extra.yaml");
        assertThat(DomainLayout.scopeDomain("platform", provider.readBranchFiles("main")))
                .containsOnlyKeys("config/namespace/platform.yaml");
        assertThat(provider.readBranchFiles("feature")).containsKey(PLATFORM_FILE);
    }

    @Test
    void droppingADomainFileDoesNotTouchSiblingDomains() {
        GitVcsClient provider = provider();
        provider.createBranch("trim", "main");

        provider.submitReview(
                new VcsReviewSubmission("analytics", "trim", "main", "Drop the report", null,
                        Map.of("config/dataset/extra.yaml", "kind: DataSet\nkeyName: extra\n")),
                "Alice", "alice@yarina.local");

        Map<String, String> branch = provider.readBranchFiles("trim");
        assertThat(branch)
                .containsKey("domains/analytics/config/dataset/extra.yaml")
                .doesNotContainKey(ANALYTICS_FILE)
                .containsKey(PLATFORM_FILE);
    }
}

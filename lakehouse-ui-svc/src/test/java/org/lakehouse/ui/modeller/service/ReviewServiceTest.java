package org.lakehouse.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;
import org.lakehouse.ui.modeller.dto.ReviewRequest;
import org.lakehouse.ui.modeller.dto.ReviewResponse;
import org.lakehouse.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.lakehouse.ui.modeller.vcs.VcsProvider;
import org.lakehouse.ui.modeller.vcs.VcsReviewResult;
import org.lakehouse.ui.modeller.vcs.VcsReviewSubmission;
import org.lakehouse.ui.modeller.workspace.BranchSelection;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewServiceTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @TempDir
    Path temp;

    private final List<VcsReviewSubmission> submissions = new ArrayList<>();

    private final Map<String, VcsReviewResult> results = new LinkedHashMap<>();

    /** Records the submissions and answers with the status registered per domain/branch key. */
    private final VcsProvider vcs = new VcsProvider() {
        @Override
        public String name() {
            return "local-git";
        }

        @Override
        public Map<String, String> readBranchFiles(String domain, String branch) {
            return Map.of();
        }

        @Override
        public List<String> listBranches(String domain) {
            return List.of();
        }

        @Override
        public void createBranch(String domain, String branch, String baseBranch) {
        }

        @Override
        public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
            submissions.add(submission);
            return results.getOrDefault(submission.domain() + "@" + submission.branch(),
                    VcsReviewResult.plainPushed("https://vcs/" + submission.domain() + "/merge/1"));
        }
    };

    private static UsernamePasswordAuthenticationToken authentication(String username) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .build();
        return new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    }

    private WorkspaceManager manager(LocalFsWorkspaceStorage storage, Map<String, Map<String, String>> seed) {
        return new WorkspaceManager(storage, (domain, branch) -> seed.getOrDefault(domain, Map.of()),
                4, Clock.fixed(T0, ZoneOffset.UTC));
    }

    @Test
    void splitsTheWorkspacePerDomainAndDeletesItOnSuccess() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));

        // a file that escaped the generated selection folders must block the whole review
        storage.writeFile(workspace.id(), "stray/config/dataset/evil.yaml", "kind: DataSet\n");
        ReviewService service = new ReviewService(manager, storage, vcs, new ModellerProperties(),
                new SyncLogService(10));

        assertThatThrownBy(() -> service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stray/config/dataset/evil.yaml");
        assertThat(submissions).isEmpty();
        assertThat(manager.exists(workspace.id())).isTrue();

        storage.deleteFile(workspace.id(), "stray/config/dataset/evil.yaml");
        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.submittedFiles()).containsOnlyKeys(
                "config/namespace/ns.yaml", "config/dataset/ds.yaml");
        assertThat(submissions).hasSize(2);
        assertThat(submissions.get(0).domain()).isEqualTo("platform");
        assertThat(submissions.get(0).files()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(submissions.get(1).domain()).isEqualTo("analytics");
        assertThat(submissions.get(1).files()).containsOnlyKeys("config/dataset/ds.yaml");
        assertThat(manager.exists(workspace.id())).isFalse();
    }

    @Test
    void keepsTheWorkspaceAndReportsNoChangesWhenEveryDomainIsUnchanged() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        WorkspaceManager manager = manager(storage, Map.of("platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n")));
        Workspace workspace = manager.openWorkspace("alice", List.of(new BranchSelection("platform", "main")));
        results.put("platform@main", VcsReviewResult.noChanges());
        ReviewService service = new ReviewService(manager, storage, vcs, new ModellerProperties(),
                new SyncLogService(10));

        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest(null, null), authentication("alice"));

        assertThat(response.status()).isEqualTo("NO_CHANGES");
        assertThat(response.url()).isNull();
        assertThat(response.submittedFiles()).isEmpty();
        assertThat(manager.exists(workspace.id())).isTrue();
    }

    @Test
    void deletesTheWorkspaceWhenAtLeastOneDomainChanged() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));
        results.put("analytics@dev", VcsReviewResult.noChanges());
        ReviewService service = new ReviewService(manager, storage, vcs, new ModellerProperties(),
                new SyncLogService(10));

        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest("commit", null), authentication("alice"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.submittedFiles()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(manager.exists(workspace.id())).isFalse();
    }
}

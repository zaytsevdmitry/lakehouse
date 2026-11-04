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

package org.yarina.domain.layout;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A business folder called {@code domains/} inside a flat, per-domain repository must not
 * turn it into a multi-domain container.
 */
class DomainLayoutTest {

    @Test
    void flatRepositoryWithABusinessDomainsFolderIsKeptWhole() {
        Map<String, String> files = Map.of(
                "domains/aaaa/datasets/report.yaml", "kind: DataSet\n",
                "schedules/initial.yaml", "kind: Schedule\n",
                "quality/metrics/transaction_qm.yaml", "kind: QualityMetric\n");

        assertThat(DomainLayout.usesDomainLayout("analytics", files)).isFalse();
        assertThat(DomainLayout.scopeDomain("analytics", files)).containsOnly(
                Map.entry("domains/aaaa/datasets/report.yaml", "kind: DataSet\n"),
                Map.entry("schedules/initial.yaml", "kind: Schedule\n"),
                Map.entry("quality/metrics/transaction_qm.yaml", "kind: QualityMetric\n"));
    }

    @Test
    void flatRepositoryWithManyBusinessDomainsFoldersIsKeptWhole() {
        Map<String, String> files = Map.of(
                "domains/ai/datasets/report.yaml", "kind: DataSet\n",
                "domains/platform/datasets/event.yaml", "kind: DataSet\n",
                "schedules/regular.yaml", "kind: Schedule\n");

        assertThat(DomainLayout.usesDomainLayout("flocktory", files)).isFalse();
        assertThat(DomainLayout.scopeDomain("flocktory", files)).isEqualTo(files);
    }

    @Test
    void flatRepositoryWhoseEveryFileLivesUnderBusinessDomainsFoldersIsKeptWhole() {
        // the real flocktory repository: all 1482 files live under domains/<business area>/,
        // there is no root-level file that could reveal the flat layout, and the domain folder
        // domains/flocktory/ does not exist
        Map<String, String> files = Map.of(
                "domains/ai/datasets/report.yaml", "kind: DataSet\n",
                "domains/masterdata_precheckout/datasets/client.yaml", "kind: DataSet\n",
                "domains/platform/tasks/check.yaml", "kind: Task\n");
        Set<String> knownDomains = Set.of("flocktory", "platform", "analytics", "processing");

        assertThat(DomainLayout.usesDomainLayout(files)).isTrue();
        assertThat(DomainLayout.usesDomainLayout("flocktory", files)).isFalse();
        assertThat(DomainLayout.usesDomainLayout("flocktory", files, knownDomains)).isFalse();
        assertThat(DomainLayout.scopeDomain("flocktory", files, knownDomains)).isEqualTo(files);
    }

    @Test
    void containerRepositoryScopesTheRequestedDomain() {
        Map<String, String> files = Map.of(
                "domains/platform/namespaces/platform.yaml", "kind: NameSpace\n",
                "domains/analytics/datasets/report.yaml", "kind: DataSet\n");

        assertThat(DomainLayout.usesDomainLayout("analytics", files)).isTrue();
        assertThat(DomainLayout.scopeDomain("analytics", files))
                .containsOnly(Map.entry("datasets/report.yaml", "kind: DataSet\n"));
        assertThat(DomainLayout.scopeDomain("platform", files))
                .containsOnly(Map.entry("namespaces/platform.yaml", "kind: NameSpace\n"));
    }

    @Test
    void repositoryWithoutTheRequestedDomainSubtreeIsKeptWhole() {
        // Without the configured domain names a repository is only scoped when it really
        // contains domains/<domain>/. Otherwise the paths may just as well be business
        // folders of a flat repository, and hiding all of them would empty the workspace
        // (the flocktory regression).
        Map<String, String> files = Map.of(
                "domains/platform/namespaces/platform.yaml", "kind: NameSpace\n");

        assertThat(DomainLayout.usesDomainLayout("processing", files)).isFalse();
        assertThat(DomainLayout.scopeDomain("processing", files)).isEqualTo(files);
    }

    @Test
    void containerRepositoryWithoutTheRequestedDomainSubtreeIsScopedEmptyWhenTheFoldersAreKnownDomains() {
        // A new domain of a multi-domain container: no subtree yet, but every folder under
        // domains/ is a configured domain, so the repository is recognized as a container
        // and the empty domain stays empty instead of pulling in its siblings.
        Map<String, String> files = Map.of(
                "domains/platform/namespaces/platform.yaml", "kind: NameSpace\n");
        Set<String> knownDomains = Set.of("platform", "analytics", "processing");

        assertThat(DomainLayout.usesDomainLayout("processing", files, knownDomains)).isTrue();
        assertThat(DomainLayout.scopeDomain("processing", files, knownDomains)).isEmpty();
    }

    @Test
    void flatRepositoryWithoutDomainsFolderIsKeptWhole() {
        Map<String, String> files = Map.of(
                "datasets/report.yaml", "kind: DataSet\n",
                "schedules/regular.yaml", "kind: Schedule\n");

        assertThat(DomainLayout.usesDomainLayout(files)).isFalse();
        assertThat(DomainLayout.scopeDomain("platform", files)).isEqualTo(files);
    }

    @Test
    void invalidDomainIsRejectedWhenTheRepositoryIsDomainScoped() {
        Map<String, String> files = Map.of(
                "domains/platform/namespaces/platform.yaml", "kind: NameSpace\n");

        assertThatThrownBy(() -> DomainLayout.scopeDomain("../escape", files))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expandDomainOnlyPrefixesInADomainScopedRepository() {
        Map<String, String> scoped = Map.of("datasets/report.yaml", "kind: DataSet\n");

        assertThat(DomainLayout.expandDomain("analytics", scoped, false))
                .containsOnly(Map.entry("datasets/report.yaml", "kind: DataSet\n"));
        assertThat(DomainLayout.expandDomain("analytics", scoped, true))
                .containsOnly(Map.entry("domains/analytics/datasets/report.yaml", "kind: DataSet\n"));
    }
}

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

package org.lakehouse.ui.modeller.auth;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModellerRoleSupportTest {

    @Test
    void noRolesMeansNoAccess() {
        assertThat(ModellerRoleSupport.effectiveRole(null)).isNull();
        assertThat(ModellerRoleSupport.effectiveRole(Set.of())).isNull();
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("SOME_OTHER_ROLE"))).isNull();
    }

    @Test
    void canonicalViewerRoleResolvesToViewer() {
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("LAKEHOUSE_MODELLER_VIEWER")))
                .isEqualTo(ModellerRole.VIEWER);
    }

    @Test
    void legacyAliasesAreAccepted() {
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("LAKEHOUSE_CONFIG_VIEWER")))
                .isEqualTo(ModellerRole.VIEWER);
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("LAKEHOUSE_CONFIG_EDITOR")))
                .isEqualTo(ModellerRole.EDITOR);
    }

    @Test
    void highestRoleWinsWithMixedRoles() {
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("LAKEHOUSE_MODELLER_VIEWER", "LAKEHOUSE_MODELLER_ADMIN")))
                .isEqualTo(ModellerRole.ADMIN);
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("LAKEHOUSE_MODELLER_VIEWER", "LAKEHOUSE_MODELLER_EDITOR")))
                .isEqualTo(ModellerRole.EDITOR);
    }

    @Test
    void roleMatchingIsCaseInsensitive() {
        assertThat(ModellerRoleSupport.effectiveRole(Set.of("  lakehouse_modeller_admin  ")))
                .isEqualTo(ModellerRole.ADMIN);
    }

    @Test
    void roleHierarchyAllowsHigherRoles() {
        assertThat(ModellerRole.VIEWER.allows(ModellerRole.VIEWER)).isTrue();
        assertThat(ModellerRole.VIEWER.allows(ModellerRole.EDITOR)).isFalse();
        assertThat(ModellerRole.EDITOR.isAtLeastEditor()).isTrue();
        assertThat(ModellerRole.ADMIN.isAtLeastEditor()).isTrue();
        assertThat(ModellerRole.VIEWER.isAtLeastEditor()).isFalse();
    }
}
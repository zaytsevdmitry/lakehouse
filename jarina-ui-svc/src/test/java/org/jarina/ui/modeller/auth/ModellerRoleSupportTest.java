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

package org.jarina.ui.modeller.auth;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModellerRoleSupportTest {

    @Test
    void noRolesMeansNoDomains() {
        assertThat(ModellerRoleSupport.domains(null)).isEmpty();
        assertThat(ModellerRoleSupport.domains(Set.of())).isEmpty();
        assertThat(ModellerRoleSupport.domains(Set.of("SOME_OTHER_ROLE", "WORKSPACE_ADMIN"))).isEmpty();
    }

    @Test
    void domainRoleIsParsedFromTheColonSeparatedRole() {
        assertThat(ModellerRoleSupport.domains(Set.of("finance:VIEWER")))
                .isEqualTo(Map.of("finance", Set.of(DomainRole.VIEWER)));
        assertThat(ModellerRoleSupport.domains(Set.of("finance:EDITOR")))
                .isEqualTo(Map.of("finance", Set.of(DomainRole.EDITOR)));
        assertThat(ModellerRoleSupport.domains(Set.of("finance:CONTROL")))
                .isEqualTo(Map.of("finance", Set.of(DomainRole.CONTROL)));
    }

    @Test
    void domainIsKeptVerbatimAndRolesMatchCaseInsensitively() {
        assertThat(ModellerRoleSupport.domains(Set.of("FINANCE:editor")))
                .isEqualTo(Map.of("FINANCE", Set.of(DomainRole.EDITOR)));
        assertThat(ModellerRoleSupport.domains(Set.of("  Finance :  Control  ")))
                .containsEntry("Finance", Set.of(DomainRole.CONTROL));
    }

    @Test
    void multipleRolesForTheSameDomainAreKeptSeparately() {
        assertThat(ModellerRoleSupport.domains(Set.of("finance:EDITOR", "finance:CONTROL")))
                .isEqualTo(Map.of("finance", Set.of(DomainRole.EDITOR, DomainRole.CONTROL)));
    }

    @Test
    void rolesWithoutTheDomainPrefixAreIgnored() {
        Map<String, Set<DomainRole>> domains = ModellerRoleSupport.domains(
                Set.of("EDITOR", "CONTROL", "WORKSPACE_ADMIN", "jarina_modeller_admin", "finance:viewer"));
        assertThat(domains).containsOnlyKeys("finance");
        assertThat(domains.get("finance")).containsExactly(DomainRole.VIEWER);
    }

    @Test
    void malformedDomainRolesAreIgnored() {
        assertThat(ModellerRoleSupport.domains(Set.of("", "   ", ":EDITOR", "finance:", "finance:BOSS")))
                .isEmpty();
    }

    @Test
    void displayLabelPrecedenceIsControlOverEditorOverViewer() {
        assertThat(ModellerRoleSupport.displayLabel(null)).isNull();
        assertThat(ModellerRoleSupport.displayLabel(Set.of())).isNull();
        assertThat(ModellerRoleSupport.displayLabel(Set.of(DomainRole.VIEWER))).isEqualTo("VIEWER");
        assertThat(ModellerRoleSupport.displayLabel(Set.of(DomainRole.VIEWER, DomainRole.EDITOR))).isEqualTo("EDITOR");
        assertThat(ModellerRoleSupport.displayLabel(
                Set.of(DomainRole.VIEWER, DomainRole.EDITOR, DomainRole.CONTROL))).isEqualTo("CONTROL");
    }

    @Test
    void domainRoleSemanticsMatchTheMatrix() {
        assertThat(DomainRole.VIEWER.canView()).isTrue();
        assertThat(DomainRole.VIEWER.canEdit()).isFalse();
        assertThat(DomainRole.VIEWER.canControl()).isFalse();

        assertThat(DomainRole.EDITOR.canView()).isTrue();
        assertThat(DomainRole.EDITOR.canEdit()).isTrue();
        assertThat(DomainRole.EDITOR.canControl()).isFalse();

        assertThat(DomainRole.CONTROL.canView()).isTrue();
        assertThat(DomainRole.CONTROL.canEdit()).isFalse();
        assertThat(DomainRole.CONTROL.canControl()).isTrue();
    }
}
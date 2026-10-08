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

import java.util.Locale;
import java.util.Set;

/**
 * Role normalization and RBAC matrix for Keycloak realm/client roles.
 * <p>
 * Canonical roles: {@code LAKEHOUSE_MODELLER_VIEWER}, {@code LAKEHOUSE_MODELLER_EDITOR},
 * {@code LAKEHOUSE_MODELLER_ADMIN}. Legacy aliases {@code LAKEHOUSE_CONFIG_VIEWER} /
 * {@code LAKEHOUSE_CONFIG_EDITOR} are accepted for backward compatibility (spec 6.2).
 */
public final class ModellerRoleSupport {

    private static final String VIEWER = "LAKEHOUSE_MODELLER_VIEWER";
    private static final String EDITOR = "LAKEHOUSE_MODELLER_EDITOR";
    private static final String ADMIN = "LAKEHOUSE_MODELLER_ADMIN";

    private static final String VIEWER_LEGACY = "LAKEHOUSE_CONFIG_VIEWER";
    private static final String EDITOR_LEGACY = "LAKEHOUSE_CONFIG_EDITOR";

    private ModellerRoleSupport() {
    }

    public static ModellerRole effectiveRole(Set<String> roles) {
        if (roles == null || roles.isEmpty())
            return null;
        boolean hasViewer = has(roles, VIEWER, VIEWER_LEGACY);
        boolean hasEditor = has(roles, EDITOR, EDITOR_LEGACY);
        boolean hasAdmin = has(roles, ADMIN);
        if (hasAdmin)
            return ModellerRole.ADMIN;
        if (hasEditor)
            return ModellerRole.EDITOR;
        if (hasViewer)
            return ModellerRole.VIEWER;
        return null;
    }

    private static boolean has(Set<String> roles, String... candidates) {
        for (String candidate : candidates) {
            for (String role : roles) {
                if (role != null && role.trim().toLowerCase(Locale.ROOT)
                        .equals(candidate.toLowerCase(Locale.ROOT)))
                    return true;
            }
        }
        return false;
    }
}
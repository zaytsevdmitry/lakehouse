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

package org.yarina.ui.modeller.auth;

import java.util.Locale;

/**
 * Access level of a Keycloak user within a single domain, stored as the
 * domain-prefixed realm role {@code <domain>:<ROLE>}.
 * <p>
 * {@code VIEWER} is implied by {@code EDITOR} and {@code CONTROL}, which are
 * orthogonal: {@code EDITOR} grants editing but not task control, while
 * {@code CONTROL} grants task control but not editing.
 */
public enum DomainRole {

    VIEWER,
    EDITOR,
    CONTROL;

    public boolean canView() {
        return true;
    }

    public boolean canEdit() {
        return this == EDITOR;
    }

    public boolean canControl() {
        return this == CONTROL;
    }

    /**
     * Parses the role part of a {@code <domain>:<ROLE>} realm role,
     * case-insensitively with surrounding whitespace ignored.
     */
    public static DomainRole parse(String role) {
        if (role == null)
            return null;
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "VIEWER":
                return VIEWER;
            case "EDITOR":
                return EDITOR;
            case "CONTROL":
                return CONTROL;
            default:
                return null;
        }
    }
}
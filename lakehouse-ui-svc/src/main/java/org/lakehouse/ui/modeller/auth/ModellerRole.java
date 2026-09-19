package org.lakehouse.ui.modeller.auth;

/**
 * Effective access level of a Keycloak user resolved from the JWT roles,
 * in ascending order: {@code VIEWER < EDITOR < ADMIN}.
 */
public enum ModellerRole {

    VIEWER,
    EDITOR,
    ADMIN;

    public boolean allows(ModellerRole required) {
        return this.compareTo(required) >= 0;
    }

    public boolean isAtLeastEditor() {
        return allows(EDITOR);
    }
}
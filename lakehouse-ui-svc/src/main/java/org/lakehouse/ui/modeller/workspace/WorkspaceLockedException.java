package org.lakehouse.ui.modeller.workspace;

/**
 * Raised when an exclusive workspace mutation (file or whole-workspace review) collides
 * with a concurrently running mutation in the same JVM. Maps to HTTP 409.
 */
public class WorkspaceLockedException extends RuntimeException {

    public WorkspaceLockedException(String message) {
        super(message);
    }
}
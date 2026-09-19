package org.lakehouse.ui.modeller.storage;

/**
 * Raised by a workspace storage backend on any unprocessable/transport failure.
 */
public class WorkspaceStorageException extends RuntimeException {

    public WorkspaceStorageException(String message) {
        super(message);
    }

    public WorkspaceStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
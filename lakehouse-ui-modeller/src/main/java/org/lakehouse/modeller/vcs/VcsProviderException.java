package org.lakehouse.modeller.vcs;

/**
 * Raised by the VCS layer on any Git transport, branch or merge-request failure.
 * Maps to HTTP 400/502 depending on caller context.
 */
public class VcsProviderException extends RuntimeException {

    public VcsProviderException(String message) {
        super(message);
    }

    public VcsProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
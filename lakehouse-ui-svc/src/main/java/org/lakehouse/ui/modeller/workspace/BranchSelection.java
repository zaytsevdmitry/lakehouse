package org.lakehouse.ui.modeller.workspace;

/**
 * A single branch picked for one configuration domain in a workspace.
 * A workspace is a set of such selections — one branch per domain repository.
 */
public record BranchSelection(String domain, String branch) {

    public String key() {
        return domain + "=" + branch;
    }

    /** Folder name of the branch checkout inside the workspace directory. */
    public String folder() {
        return domain + " (" + branch + ")";
    }

    /** True for a selection whose files live under the given workspace path. */
    public boolean covers(String path) {
        return folder().equals(path) || path.startsWith(folder() + "/");
    }
}
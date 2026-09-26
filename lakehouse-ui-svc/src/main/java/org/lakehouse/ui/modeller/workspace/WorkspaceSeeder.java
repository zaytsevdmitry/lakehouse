package org.lakehouse.ui.modeller.workspace;

import java.util.Map;

/**
 * Seeds a freshly created workspace with the metadata files of one selected branch
 * (of one domain repository). Implemented by the active {@code VcsProvider} in production
 * wiring. The returned paths are the paths within the domain repository (scoped to the
 * domain subtree when the repository hosts several domains).
 */
@FunctionalInterface
public interface WorkspaceSeeder {

    /**
     * @param domain configuration domain whose repository is read
     * @param branch branch to read
     * @return branch content as path-to-YAML map (may be empty for empty branches)
     */
    Map<String, String> snapshot(String domain, String branch);
}
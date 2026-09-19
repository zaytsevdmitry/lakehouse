package org.lakehouse.ui.modeller.workspace;

import java.util.Map;

/**
 * Seeds a freshly created workspace with the metadata files of the requested branch.
 * Implemented by the active {@code VcsProvider} in production wiring.
 */
@FunctionalInterface
public interface WorkspaceSeeder {

    /**
     * @return branch content as path-to-YAML map (may be empty for empty branches).
     */
    Map<String, String> snapshot(String branch);
}
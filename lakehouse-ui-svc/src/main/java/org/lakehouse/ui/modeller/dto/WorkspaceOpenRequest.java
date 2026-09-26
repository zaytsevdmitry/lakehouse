package org.lakehouse.ui.modeller.dto;

import org.lakehouse.ui.modeller.workspace.BranchSelection;

import java.util.List;

/**
 * Body for opening (creating) a workspace on a set of branches — one per selected
 * domain repository. Unlisted domains are simply not part of the workspace.
 */
public record WorkspaceOpenRequest(
        List<BranchSelection> branches) {
}
package org.lakehouse.modeller.dto;

import java.util.List;

/**
 * Body for opening (creating) a workspace on a given branch.
 */
public record WorkspaceOpenRequest(
        String branch,
        List<String> files) {
}
package org.lakehouse.task.proxy.spark.dto;

public enum ExternalStatus {

    WAITING,
    RUNNING,
    FINISHED,
    FAILED,
    KILLED,
    UNKNOWN;

    public static ExternalStatus fromInternal(String internalStatus) {
        if (internalStatus == null) return UNKNOWN;
        return switch (internalStatus) {
            case "QUEUED", "CLAIMED", "SUBMITTED" -> WAITING;
            case "COMPLETED" -> FINISHED;
            case "FAILED" -> FAILED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromK8sPhase(String phase) {
        if (phase == null) return UNKNOWN;
        return switch (phase) {
            case "Pending" -> WAITING;
            case "Running" -> RUNNING;
            case "Succeeded" -> FINISHED;
            case "Failed" -> FAILED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromStandaloneState(String state) {
        if (state == null) return UNKNOWN;
        return switch (state.toUpperCase()) {
            case "SUBMITTED", "RELAUNCHING" -> WAITING;
            case "RUNNING" -> RUNNING;
            case "FINISHED" -> FINISHED;
            case "FAILED", "ERROR" -> FAILED;
            case "KILLED" -> KILLED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromYarnState(String state) {
        if (state == null) return UNKNOWN;
        return switch (state.toUpperCase()) {
            case "NEW", "NEW_SAVING", "SUBMITTED", "ACCEPTED" -> WAITING;
            case "RUNNING" -> RUNNING;
            case "FINISHED" -> FINISHED;
            case "FAILED" -> FAILED;
            case "KILLED" -> KILLED;
            default -> UNKNOWN;
        };
    }
}

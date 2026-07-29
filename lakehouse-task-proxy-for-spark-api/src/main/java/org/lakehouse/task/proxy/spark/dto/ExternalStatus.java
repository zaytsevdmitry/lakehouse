package org.lakehouse.task.proxy.spark.dto;

public enum ExternalStatus {

    WAITING,
    SUBMITTED,
    RUNNING,
    FINISHED,
    FAILED,
    ERROR,
    KILLED,
    UNKNOWN;

    public static ExternalStatus fromInternal(String internalStatus) {
        if (internalStatus == null) return UNKNOWN;
        try {
            return valueOf(internalStatus);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static ExternalStatus fromK8sPhase(String phase) {
        if (phase == null) return UNKNOWN;
        return switch (phase) {
            case "Pending" -> SUBMITTED;
            case "Running" -> RUNNING;
            case "Succeeded" -> FINISHED;
            case "Failed" -> FAILED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromStandaloneState(String state) {
        if (state == null) return UNKNOWN;
        return switch (state.toUpperCase()) {
            case "SUBMITTED", "RELAUNCHING" -> SUBMITTED;
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
            case "NEW", "NEW_SAVING", "SUBMITTED", "ACCEPTED" -> SUBMITTED;
            case "RUNNING" -> RUNNING;
            case "FINISHED" -> FINISHED;
            case "FAILED" -> FAILED;
            case "KILLED" -> KILLED;
            default -> UNKNOWN;
        };
    }
}

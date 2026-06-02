package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

public enum PodState {
    UNKNOWN("UNKNOWN"),
    SUBMITTED("SUBMITTED"),
    RUNNING("RUNNING"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    TIMEOUT("TIMEOUT");

    private final String value;

    PodState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    public boolean equals(String state) {
        return this.value.equalsIgnoreCase(state);
    }
    public static PodState fromString(String text) {
        if (text == null) {
            return UNKNOWN;
        }
        for (PodState state : PodState.values()) {
            if (state.value.equalsIgnoreCase(text)) {
                return state;
            }
        }
        return UNKNOWN;
    }
}

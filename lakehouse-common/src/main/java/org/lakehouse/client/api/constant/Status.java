package org.lakehouse.client.api.constant;

public class Status {
    public enum Schedule {
        NEW("NEW"),
        RUNNING("RUNNING"),
        FAILED("FAILED"),
        SUCCESS("SUCCESS");

        public final String label;

        Schedule(String label) {
            this.label = label;
        }

    }

    public enum Task{
        NEW("NEW"),
        QUEUED("QUEUED"),
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        CONF_ERROR("CONF_ERROR");

        public final String label;

        Task(String label) {
            this.label = label;
        }
    }

    public enum ScenarioAct{
        NEW("NEW"),
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED");

        public final String label;

        ScenarioAct(String label) {
            this.label = label;
        }

    }

}

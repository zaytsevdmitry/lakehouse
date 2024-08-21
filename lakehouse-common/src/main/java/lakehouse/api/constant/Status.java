package lakehouse.api.constant;

public class Status {
    public enum Schedule {
         NEW("NEW"),
        READY_ACTS("READY_ACTS"),
        RUNNING("RUNNING"),
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
        SUCCESS("SUCCESS");

        public final String label;

        Task(String label) {
            this.label = label;
        }

    }

    public enum ScenarioAct{
        NEW("NEW"),
        QUEUED("QUEUED"),
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS");

        public final String label;

        ScenarioAct(String label) {
            this.label = label;
        }

    }

}

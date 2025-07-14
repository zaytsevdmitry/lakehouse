package org.lakehouse.client.api.constant;

public class Types {
    public enum DQMetricsType {
        CONSTRAINT("constraint"),
        SPARK_SQL("sparkSQL"),
        PUSH_DOWN_SQL("pushDownSQL"),
        CLASS("class");

        public final String label;

        DQMetricsType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}

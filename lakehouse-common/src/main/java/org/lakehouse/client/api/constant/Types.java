package org.lakehouse.client.api.constant;

public class Types {

    public enum DataSourceType {
        file("file"),
        iceberg("iceberg"),
        database("database");
        public final String label;

        DataSourceType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    public enum ConnectionType {
        spark("spark"),
        jdbc("jdbc");

        public final String label;

        ConnectionType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    public enum Constraint {
        primary("primary"),
        foreign("foreign"),
        unique("unique"),
        check("check");

        public final String label;

        Constraint(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum ConstraintLevelCheck{
        dataQuality("dataQuality"), // used only with DQ
        construct("construct"), // try to perform construct on the table
        none("none"); // ignored
        public final String label;
        ConstraintLevelCheck(String label){this.label = label;}

        @Override
        public String toString() {
            return  label;
        }
    }

    public enum ReferenceAction{
        SET_NULL("SET NULL"),
        DEFAULT("SET DEFAULT"),
        RESTRICT("RESTRICT"),
        NO_ACTION("NO ACTION"),
        CASCADE("CASCADE");

        private final String value;
        ReferenceAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
    public enum DQMetricsType {
        constraint("constraint"),
        sparkSQL("sparkSQL"),
        pushDownSQL("pushDownSQL"),
        objectClass("objectClass");

        public final String label;

        DQMetricsType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DQThresholdViolationLevel {
        error("error"),
        info("info");
        public final String label;

        DQThresholdViolationLevel(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}

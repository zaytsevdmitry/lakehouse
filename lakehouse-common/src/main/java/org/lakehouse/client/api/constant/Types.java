package org.lakehouse.client.api.constant;

public class Types {
    public enum DataSourceType {
        filesystem("filesystem"),
        restapi("restapi"),
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

    public enum DataSourceServiceType {
        hdfs("hdfs"),
        localfs("localfs"),
        postgres("postgres"),
        trino("trino");
        public final String label;

        DataSourceServiceType(String label) {
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

}

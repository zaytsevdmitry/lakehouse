package org.lakehouse.client.api.constant;

public class Types {
    public enum EngineType {
        spark("spark"),
        restapi("restapi"),
        database("database");
        public final String label;

        EngineType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Engine {
        json("json"),
        parquet("parquet"),
        orc("orc"),
        csv("csv"),
        text("text"),
        iceberg("iceberg"),
        localfs("localfs"),
        postgres("postgres"),
        trino("trino");
        public final String label;

        Engine(String label) {
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

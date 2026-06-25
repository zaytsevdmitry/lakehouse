/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    public enum DQMetricTestSetType {
        integrity("integrity"),
        sparkSQL("sparkSQL"),
        pushDownSQL("pushDownSQL"),
        objectClass("objectClass");

        public final String label;

        DQMetricTestSetType(String label) {
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

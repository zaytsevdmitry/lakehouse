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

import com.fasterxml.jackson.annotation.JsonValue;

public class Types {

    public enum DataSourceType {
        FILE("file"),
        ICEBERG("iceberg"),
        DATABASE("database");

        public final String label;

        DataSourceType(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum ConnectionType {
        SPARK("SPARK"),
        JDBC("JDBC");

        public final String label;

        ConnectionType(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum ConstraintType {
        PRIMARY("primary"),
        FOREIGN("foreign"),
        UNIQUE("unique"),
        CHECK("check");

        public final String label;

        ConstraintType(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum ConstraintLevelCheck {
        DATA_QUALITY("dataQuality"), // used only with DQ
        CONSTRUCT("construct"),       // try to perform construct on the table
        NONE("none");                 // ignored

        public final String label;

        ConstraintLevelCheck(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum ReferenceAction {
        SET_NULL("SET NULL"),
        DEFAULT("SET DEFAULT"),
        RESTRICT("RESTRICT"),
        NO_ACTION("NO ACTION"),
        CASCADE("CASCADE");

        public final String label;

        ReferenceAction(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum DQMetricTestSetType {
        INTEGRITY("integrity"),
        SPARK_SQL("sparkSQL"),
        PUSH_DOWN_SQL("pushDownSQL"),
        OBJECT_CLASS("objectClass");

        public final String label;

        DQMetricTestSetType(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }

    public enum DQThresholdViolationLevel {
        ERROR("error"),
        INFO("info");

        public final String label;

        DQThresholdViolationLevel(String label) {
            this.label = label;
        }

        @Override
        @JsonValue
        public String toString() {
            return label;
        }
    }
}

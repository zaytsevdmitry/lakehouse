/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

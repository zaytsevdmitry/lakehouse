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

public class Status {
    public enum Schedule {
        NEW("NEW"),
        RUNNING("RUNNING"),
        FAILED("FAILED"),
        SUCCESS("SUCCESS"),
        CONF_ERROR("CONF_ERROR");

        public final String label;

        Schedule(String label) {
            this.label = label;
        }

    }

    public enum Task {
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

    public enum ScenarioAct {
        NEW("NEW"),
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED");

        public final String label;

        ScenarioAct(String label) {
            this.label = label;
        }
    }

    public enum DataSet {
        LOCKED("LOCKED"),
        SUCCESS("SUCCESS");

        public final String label;

        DataSet(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
    public enum DQMetric {
        RUNNING("RUNNING"),
        SUCCESS("SUCCESS"),
        ERROR("ERROR"),
        FAILED("FAILED");

        public final String label;

        DQMetric(String label) {
            this.label = label;
        }
    }

}

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

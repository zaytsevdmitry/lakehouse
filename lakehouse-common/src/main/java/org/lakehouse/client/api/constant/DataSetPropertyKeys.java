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

public class DataSetPropertyKeys {
    public enum Key {
        LOCATION("location"),
        TRANSFORM_LOCATION ( "transform.location"),
        TRANSFORM_LOCATION_MODE ("transform.location.mode"),
        FORMAT("format"),
        USING("using");


        Key(String label) {
            this.label = label;
        }

        public final String label;

        @Override
        public String toString() {
            return label;
        }
    }
    public enum Prefix {
        dataSet("dataset."),
        dataSetWrite("dataset.write."),
        dataSetRead("dataset.read."),
        transform("transform."),
        transformWrite("transform.write."),
        transformRead("transform.read.");

        public final String label;

        Prefix(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;}

    }
}

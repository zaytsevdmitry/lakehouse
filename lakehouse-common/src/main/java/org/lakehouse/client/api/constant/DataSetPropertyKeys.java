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

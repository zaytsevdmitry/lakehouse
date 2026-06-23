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

public class SystemVarKeys {

    /**
     * global context keys
     * */
    public static String TARGET_DATE_TIME_TZ_KEY = "targetDateTime";
    public static String TARGET_INTERVAL_START_TZ_KEY = "intervalStartDateTime";
    public static String TARGET_INTERVAL_END_TZ_KEY = "intervalEndDateTime";
    public static String TARGET_DATASET_KEY_NAME = "targetDataSetKeyName"; //


    /**
    * localContext keys
    */
    public static String CURRENT_DATASET_KEY_NAME = "currentDataSetKeyName";
    public static String CONSTRAINT_NAME = "constraintName";
    public static String CONSTRAINT = "constraint";
    public static String PARTITION_NAME = "partitionName";
    public static String SCRIPT = "script";
    public static String DATASOURCE_SERVICE_PROTOCOL_NAME_KEY = "datasource.service.protocol"; // jdbc:[*]/spark/http[s] etc
    public static String SERVICE_KEY = "service";
    public static String DRIVER_KEY = "driver";
    public static String DATASOURCE_KEY = "dataSource";
    public static String DATASET_KEY = "dataSet";
    public static String COLUMN = "column";


    /**
     * internal use constants
     * */
    public static String SCRIPT_DELIMITER = "\n;";
}

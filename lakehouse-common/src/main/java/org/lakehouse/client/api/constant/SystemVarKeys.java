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

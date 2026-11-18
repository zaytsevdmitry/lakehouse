/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.client.api.constant;

public class SystemVarKeys {

    /**
     * global context keys
     * */
    public static String TARGET_DATE_TIME_TZ_KEY = "targetDateTime";
    public static String TARGET_INTERVAL_START_TZ_KEY = "intervalStartDateTime";
    public static String TARGET_INTERVAL_END_TZ_KEY = "intervalEndDateTime";
    public static String TARGET_INTERVAL_START_DATE_KEY = "intervalStartDate";
    public static String TARGET_INTERVAL_END_DATE_KEY = "intervalEndDate";
    public static String TARGET_DATASET_KEY_NAME = "targetDataSetKeyName"; //
    public static String QUALITY_METRICS_CONF_KEY_NAME = "qualityMetricsConfKeyName";


    /**
    * localContext keys
    */
    public static String CURRENT_DATASET_KEY_NAME = "currentDataSetKeyName";
    public static String CONSTRAINT_NAME = "constraintName";
    public static String CONSTRAINT = "constraint";
    public static String CONSTRAINT_COLUMNS = "constraintColumns";
    public static String CONSTRAINT_SEVERITY = "constraintSeverity";
    public static String PARTITION_NAME = "partitionName";
    public static String SCRIPT = "script";
    public static String SERVICE_KEY = "service";
    public static String DRIVER_KEY = "driver";
    public static String DATASOURCE_KEY = "dataSource";
    public static String DATASET_KEY = "dataSet";
    public static String COLUMN = "column";

    public static String FOREIGN_REFERENCE_DATA_SET_KEY_NAME = "referenceDataSetKeyName";
    public static String FOREIGN_REFERENCE_CONSTRAINT_NAME = "referenceConstraintName";
    public static String FOREIGN_SOURCE_COLUMNS = "sourceColumns";
    public static String FOREIGN_REFERENCE_COLUMNS = "referenceColumns";
    public static String FOREIGN_SOURCE_COLUMN_TYPES = "sourceColumnTypes";
    public static String FOREIGN_REFERENCE_COLUMN_TYPES = "referenceColumnTypes";
    public static String FOREIGN_SOURCE_CAST_TYPES = "sourceCastTypes";
    public static String FOREIGN_TYPE_MISMATCH = "foreignTypeMismatch";
    public static String FOREIGN_TYPE_MISMATCH_MESSAGE = "foreignTypeMismatchMessage";


    /**
     * internal use constants
     * */
    public static String SCRIPT_DELIMITER = "\n;";
}

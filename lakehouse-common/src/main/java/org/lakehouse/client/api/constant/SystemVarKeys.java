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
    public static String PARTITION_NAME = "partitionName";
    public static String SCRIPT = "script";
    public static String CONNECTION_STRING_PROTOCOL_NAME = "protocol"; // jdbc:[*]/spark/http[s] etc
    public static String SERVICE_KEY = "service";
    public static String DRIVER_KEY = "driver";
    public static String DATASOURCE_KEY = "dataSource";
    public static String DATASET_KEY = "dataSet";


    /**
     * internal use constants
     * */
    public static String SCRIPT_DELIMITER = "\n;";
}

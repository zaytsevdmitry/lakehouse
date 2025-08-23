package org.lakehouse.client.api.constant;

public class SystemVarKeys {
    public static String TARGET_DATE_TIME_TZ_KEY = "targetDateTimeTZ";
    public static String TARGET_INTERVAL_START_TZ_KEY = "targetIntervalStartTZ";
    public static String TARGET_INTERVAL_END_TZ_KEY = "targetIntervalEndTZ";

    public static String SOURCE_KEY_F = "source_%s_%s_";
    public static String SOURCE_TABLE_FULL_NAME_KEY_F = SOURCE_KEY_F.concat("tableFullName");
    public static String SOURCE_KEY_NAME_KEY_F = SOURCE_KEY_F.concat("keyName");

    public static String buildSourceTableFullName(String project, String dataSet){
        return String.format(SOURCE_TABLE_FULL_NAME_KEY_F, project, dataSet);
    }

    public static String buildSourceKeyName(String project, String dataSet){
        return String.format(SOURCE_KEY_NAME_KEY_F, project, dataSet);
    }
}

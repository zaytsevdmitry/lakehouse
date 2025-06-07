package org.lakehouse.jinjava;


import org.lakehouse.client.api.utils.DateTimeUtils;

public class JinjavaDateTimeFunctions {
    public static String addDaysISO(String dateTimeStr, Integer days){
        if (dateTimeStr == null)
            return "";
        else
            return DateTimeUtils.formatDateTimeFormatWithTZ(
                    DateTimeUtils.parseDateTimeFormatWithTZ(dateTimeStr).plusDays(days));
    }
}

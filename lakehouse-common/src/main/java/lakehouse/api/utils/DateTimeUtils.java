package lakehouse.api.utils;

import lakehouse.api.exception.CronParceErrorException;
import org.springframework.scheduling.support.CronExpression;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

    public static final DateTimeFormatter dateTimeFormatWithTZ =  DateTimeFormatter.ISO_OFFSET_DATE_TIME;


    public static OffsetDateTime getNextTargetExecutionDateTime(String cronExpressionStr, OffsetDateTime lastTargetExecutionDateTime) throws CronParceErrorException {
        try {
            CronExpression expression = CronExpression.parse(cronExpressionStr);
            return expression.next(lastTargetExecutionDateTime);
        } catch (Exception e) {
            throw new CronParceErrorException(String.format("Check value %s", cronExpressionStr), e);
        }
    }


    public static OffsetDateTime parceDateTimeFormatWithTZ(String s){
        if (s == null)
            return null;
        else
            return OffsetDateTime.parse(s,dateTimeFormatWithTZ);
    }


    public static String formatDateTimeFormatWithTZ(OffsetDateTime offsetDateTime){
        if (offsetDateTime == null)
            return null;
        else
            return offsetDateTime.format(dateTimeFormatWithTZ);
    }


}

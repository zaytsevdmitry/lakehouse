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

package org.lakehouse.client.api.utils;

import org.lakehouse.client.api.exception.CronParceErrorException;
import org.springframework.scheduling.support.CronExpression;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class DateTimeUtils {

    public static final DateTimeFormatter dateTimeFormatWithTZ = DateTimeFormatter.ISO_OFFSET_DATE_TIME;


    public static OffsetDateTime getNextTargetExecutionDateTime(String cronExpressionStr, OffsetDateTime lastTargetExecutionDateTime) throws CronParceErrorException {
        try {
            CronExpression expression = CronExpression.parse(cronExpressionStr);
            return expression.next(lastTargetExecutionDateTime);
        } catch (Exception e) {
            throw new CronParceErrorException(String.format("Check value %s", cronExpressionStr), e);
        }
    }


    public static OffsetDateTime parseDateTimeFormatWithTZ(String s) {
        if (s == null || s.isBlank())
            return null;
        else
            return OffsetDateTime.parse(s, dateTimeFormatWithTZ);
    }


    public static String formatDateTimeFormatWithTZ(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null)
            return null;
        else
            return offsetDateTime.format(dateTimeFormatWithTZ);
    }

    public static OffsetDateTime now() {
        return OffsetDateTime.now();
    }

    public static String nowStr() {
        return formatDateTimeFormatWithTZ(now());
    }

    public static boolean strEquals(String strDT1, String strDT2) {
        return Objects.equals(
                parseDateTimeFormatWithTZ(strDT1),
                parseDateTimeFormatWithTZ(strDT2));
    }
}

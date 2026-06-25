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

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

package org.yarina.taskexecutor.spark.dq.runner.integrity;

import org.yarina.client.api.constant.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Formats integrity diagnostics. Kept free of Spark dependencies so that user-facing error messages
 * can be unit tested without starting a Spark session.
 */
public final class IntegrityDiagnostics {

    private IntegrityDiagnostics() {
    }

    public static String constraintMessage(
            String constraintName,
            Types.ConstraintType type,
            String dataSetKeyName,
            String columns,
            String originalMessage) {
        String typeLabel = type == null ? "nullable" : type.label;
        return String.format("Constraint %s (%s), dataset %s, column(s) %s: %s",
                constraintName, typeLabel, dataSetKeyName, blankToDash(columns), rootMessage(originalMessage));
    }

    public static String columnMessage(String columnName, String dataSetKeyName, String originalMessage) {
        return String.format("Constraint %s (nullable), dataset %s, column(s) %s: %s",
                columnName, dataSetKeyName, columnName, rootMessage(originalMessage));
    }

    /**
     * Explicit type mismatch diagnostic for foreign keys, e.g.
     * {@code FK transaction_processing_client_fk: type mismatch — source client_id (string) vs reference client_processing.id (integer)}.
     */
    public static String fkTypeMismatch(
            String constraintName,
            String referenceDataSetKeyName,
            List<String> sourceColumns,
            List<String> sourceTypes,
            List<String> referenceColumns,
            List<String> referenceTypes) {
        List<String> mismatches = new ArrayList<>();
        int size = Math.min(Math.min(sourceColumns.size(), sourceTypes.size()),
                Math.min(referenceColumns.size(), referenceTypes.size()));
        for (int i = 0; i < size; i++) {
            if (typeMismatch(sourceTypes.get(i), referenceTypes.get(i))) {
                mismatches.add(String.format("source %s (%s) vs reference %s.%s (%s)",
                        sourceColumns.get(i), blankToDash(sourceTypes.get(i)),
                        referenceDataSetKeyName, referenceColumns.get(i),
                        blankToDash(referenceTypes.get(i))));
            }
        }
        if (mismatches.isEmpty()) {
            return "";
        }
        return String.format("FK %s: type mismatch — %s", constraintName, String.join(", ", mismatches));
    }

    public static boolean typeMismatch(String sourceType, String referenceType) {
        return !normalizedType(sourceType).equals(normalizedType(referenceType));
    }

    /**
     * Normalizes a column type so that platform-specific aliases compare equal.
     */
    public static String normalizedType(String type) {
        if (type == null) {
            return "";
        }
        String normalized = type.trim().toLowerCase(Locale.ROOT);
        int parameterStart = normalized.indexOf('(');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart).trim();
        }
        return switch (normalized) {
            case "string", "varchar", "varchar2", "char", "nchar", "text", "nvarchar", "clob" -> "string";
            case "int", "integer", "int4", "smallint", "short" -> "int";
            case "long", "bigint", "int8" -> "bigint";
            case "decimal", "numeric", "number" -> "decimal";
            case "double", "double precision", "float8" -> "double";
            case "float", "real", "float4" -> "float";
            case "bool", "boolean" -> "boolean";
            case "date" -> "date";
            case "timestamp", "datetime", "timestamp without time zone", "timestamp with time zone",
                    "timestamptz" -> "timestamp";
            default -> normalized;
        };
    }

    private static String rootMessage(String message) {
        return message == null || message.isBlank() ? "unknown error" : message;
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}

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

import org.junit.jupiter.api.Test;
import org.yarina.client.api.constant.Types;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntegrityDiagnosticsTest {

    @Test
    void constraintMessageContainsContext() {
        String message = IntegrityDiagnostics.constraintMessage(
                "transaction_processing_client_fk",
                Types.ConstraintType.FOREIGN,
                "transaction_dds",
                "client_id",
                "Table or view not found: client_processing");

        assertTrue(message.contains("transaction_processing_client_fk"));
        assertTrue(message.contains("foreign"));
        assertTrue(message.contains("transaction_dds"));
        assertTrue(message.contains("client_id"));
        assertTrue(message.contains("Table or view not found: client_processing"));
    }

    @Test
    void constraintMessageWithoutConstraintNameOrColumns() {
        String message = IntegrityDiagnostics.constraintMessage(
                null, Types.ConstraintType.CHECK, "transaction_dds", null, null);

        assertTrue(message.contains("Constraint null (check)"));
        assertTrue(message.contains("column(s) -"));
        assertTrue(message.contains("unknown error"));
    }

    @Test
    void columnMessageForNullableChecks() {
        String message = IntegrityDiagnostics.columnMessage(
                "commission", "transaction_dds", "Cannot cast string to decimal");

        assertTrue(message.contains("Constraint commission (nullable)"));
        assertTrue(message.contains("dataset transaction_dds"));
        assertTrue(message.contains("column(s) commission"));
        assertTrue(message.contains("Cannot cast string to decimal"));
    }

    @Test
    void fkTypeMismatchListsMismatchedColumns() {
        String message = IntegrityDiagnostics.fkTypeMismatch(
                "transaction_processing_client_fk",
                "client_processing",
                List.of("client_id", "region_id"),
                List.of("string", "integer"),
                List.of("id", "region_id"),
                List.of("integer", "integer"));

        assertTrue(message.startsWith("FK transaction_processing_client_fk: type mismatch"));
        assertTrue(message.contains("source client_id (string) vs reference client_processing.id (integer)"));
        assertFalse(message.contains("region_id"));
    }

    @Test
    void fkTypeMismatchIsEmptyWhenTypesMatch() {
        String message = IntegrityDiagnostics.fkTypeMismatch(
                "transaction_processing_client_fk",
                "client_processing",
                List.of("client_id"),
                List.of("integer"),
                List.of("id"),
                List.of("int4"));

        assertEquals("", message);
    }

    @Test
    void typeMismatchNormalizesPlatformAliases() {
        assertFalse(IntegrityDiagnostics.typeMismatch("VARCHAR(255)", "string"));
        assertFalse(IntegrityDiagnostics.typeMismatch("int4", "INTEGER"));
        assertFalse(IntegrityDiagnostics.typeMismatch("numeric(18, 2)", "decimal"));
        assertFalse(IntegrityDiagnostics.typeMismatch("timestamptz", "timestamp"));
        assertFalse(IntegrityDiagnostics.typeMismatch("bool", "boolean"));
        assertTrue(IntegrityDiagnostics.typeMismatch("string", "integer"));
        assertTrue(IntegrityDiagnostics.typeMismatch(null, "integer"));
    }

    @Test
    void normalizedTypeHandlesUnknownTypesAndParameters() {
        assertEquals("geometry", IntegrityDiagnostics.normalizedType(" Geometry "));
        assertEquals("string", IntegrityDiagnostics.normalizedType("varchar(255)"));
        assertEquals("", IntegrityDiagnostics.normalizedType(null));
    }
}

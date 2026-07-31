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
package org.lakehouse.task.proxy.spark.dto;

public enum ExternalStatus {

    WAITING,
    SUBMITTED,
    RUNNING,
    FINISHED,
    FAILED,
    ERROR,
    KILLED,
    UNKNOWN;

    public static ExternalStatus fromInternal(String internalStatus) {
        if (internalStatus == null) return UNKNOWN;
        try {
            return valueOf(internalStatus);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static ExternalStatus fromK8sPhase(String phase) {
        if (phase == null) return UNKNOWN;
        return switch (phase) {
            case "Pending" -> SUBMITTED;
            case "Running" -> RUNNING;
            case "Succeeded" -> FINISHED;
            case "Failed" -> FAILED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromStandaloneState(String state) {
        if (state == null) return UNKNOWN;
        return switch (state.toUpperCase()) {
            case "SUBMITTED", "RELAUNCHING" -> SUBMITTED;
            case "RUNNING" -> RUNNING;
            case "FINISHED" -> FINISHED;
            case "FAILED", "ERROR" -> FAILED;
            case "KILLED" -> KILLED;
            default -> UNKNOWN;
        };
    }

    public static ExternalStatus fromYarnState(String state) {
        if (state == null) return UNKNOWN;
        return switch (state.toUpperCase()) {
            case "NEW", "NEW_SAVING", "SUBMITTED", "ACCEPTED" -> SUBMITTED;
            case "RUNNING" -> RUNNING;
            case "FINISHED" -> FINISHED;
            case "FAILED" -> FAILED;
            case "KILLED" -> KILLED;
            default -> UNKNOWN;
        };
    }
}

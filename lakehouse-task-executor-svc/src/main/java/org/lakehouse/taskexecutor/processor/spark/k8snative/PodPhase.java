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

package org.lakehouse.taskexecutor.processor.spark.k8snative;

public enum PodPhase {
    UNKNOWN("UNKNOWN"),
    SUBMITTED("SUBMITTED"), // Внутреннее состояние: отправлен в API
    PENDING("PENDING"),     // Фаза K8s: Ожидание ресурсов/пула контейнеров
    RUNNING("RUNNING"),     // Фаза K8s: Контейнер запущен
    SUCCEEDED("SUCCEEDED"), // Фаза K8s: Успешно завершен
    FAILED("FAILED"),       // Фаза K8s: Завершен с ошибкой
    TIMEOUT("TIMEOUT");     // Внутреннее состояние: превышено время ожидания

    private final String value;

    PodPhase(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean equalsString(String state) {
        return this.value.equalsIgnoreCase(state);
    }

    public static PodPhase fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        String cleanText = text.trim().toUpperCase();
        
        // Маппинг стандартной фазы Kubernetes "Succeeded" на наш enum
        if ("SUCCEEDED".equals(cleanText)) {
            return SUCCEEDED;
        }

        for (PodPhase phase : PodPhase.values()) {
            if (phase.value.equals(cleanText)) {
                return phase;
            }
        }
        return UNKNOWN;
    }
}

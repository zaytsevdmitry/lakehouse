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

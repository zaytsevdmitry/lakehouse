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

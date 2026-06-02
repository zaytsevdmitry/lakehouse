package org.lakehouse.taskexecutor.test;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.processor.spark.k8s.operator.K8sSparkOperatorClientUtils;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@EnableKubernetesMockClient(crud = true)
class K8sSparkOperatorClientUtilsTest {

    // Поле заполняется автоматически благодаря аннотации @EnableKubernetesMockClient
    KubernetesClient client;
    K8sSparkOperatorClientUtils utils;

    private static final ResourceDefinitionContext CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup("sparkoperator.k8s.io")
            .withVersion("v1beta2")
            .withKind("SparkApplication")
            .withNamespaced(true)
            .build();

    @BeforeEach
    void setUp() {
        utils = new K8sSparkOperatorClientUtils();

        // Регистрируем схему CRD в Mock-сервере один раз для всех тестов
        client.apiextensions().v1().customResourceDefinitions().load(new ByteArrayInputStream("""
                {
                  "apiVersion": "apiextensions.k8s.io/v1",
                  "kind": "CustomResourceDefinition",
                  "metadata": { "name": "sparkapplications.sparkoperator.k8s.io" },
                  "spec": {
                    "group": "sparkoperator.k8s.io",
                    "names": { "kind": "SparkApplication", "plural": "sparkapplications" },
                    "scope": "Namespaced",
                    "versions": [{ 
                        "name": "v1beta2", "served": true, "storage": true,
                        "schema": { "openAPIV3Schema": { "type": "object", "x-kubernetes-preserve-unknown-fields": true } } 
                    }]
                  }
                }""".getBytes())).create();
    }

    @Test
    @DisplayName("Should execute successfully when task goes through RUNNING to COMPLETED")
    void shouldSubmitSuccessfully() {
        String jobName = "success-job";
        String jsonSpec = String.format("""
                {
                  "apiVersion": "sparkoperator.k8s.io/v1beta2",
                  "kind": "SparkApplication",
                  "metadata": { "name": "%s", "namespace": "default" },
                  "spec": { "type": "Java" }
                }""", jobName);

        // Имитируем реальный жизненный цикл: появление -> RUNNING через 200мс -> COMPLETED через 500мс
        simulateOperatorLifecycle(jobName, "COMPLETED", 200, 500);

        // Метод должен успешно завершиться
        assertDoesNotThrow(() -> utils.submit(client, jsonSpec,2,6));

        // Проверяем авто-очистку: успешная задача должна быть удалена из etcd
        assertNull(client.genericKubernetesResources(CONTEXT).inNamespace("default").withName(jobName).get());
    }

    @Test
    @DisplayName("Should throw TaskFailedException when Spark job fails")
    void shouldThrowExceptionWhenStatusIsFailed() {
        String jobName = "failed-job";
        String jsonSpec = String.format("""
                {
                  "apiVersion": "sparkoperator.k8s.io/v1beta2",
                  "kind": "SparkApplication",
                  "metadata": { "name": "%s", "namespace": "default" },
                  "spec": { "type": "Java" }
                }""", jobName);

        simulateOperatorLifecycle(jobName, "FAILED", 100, 300);

        TaskFailedException exception = assertThrows(TaskFailedException.class, () -> utils.submit(client, jsonSpec, 2,6));
        System.out.println(exception.getMessage());
        assertTrue(exception.getMessage().contains("failed in Kubernetes"));

    }

    /**
     * Полноценный симулятор Spark-оператора, отрабатывающий асинхронно в фоне.
     * Позволяет проверить устойчивость кода к промежуточным Null-статусам.
     */
    private void simulateOperatorLifecycle(String name, String terminalState, long toRunningDelayMs, long toTerminalDelayMs) {
        new Thread(() -> {
            try {
                var resOp = client.genericKubernetesResources(CONTEXT)
                        .inNamespace("default")
                        .withName(name);

                // Ждем появления ресурса в etcd Mock-сервера
                GenericKubernetesResource current = null;
                for (int i = 0; i < 10; i++) {
                    current = resOp.get();
                    if (current != null) break;
                    Thread.sleep(50);
                }

                if (current == null) return;

                // ЭТАП 1: Переводим задачу в статус RUNNING
                Thread.sleep(toRunningDelayMs);
                current = resOp.get();
                current.setAdditionalProperties(Map.of(
                        "status", Map.of("applicationState", Map.of("state", "RUNNING"))
                ));
                resOp.patchStatus(current);

                // ЭТАП 2: Переводим задачу в терминальный статус (COMPLETED или FAILED)
                Thread.sleep(toTerminalDelayMs);
                current = resOp.get();
                if (current != null) { // Объект мог быть удален, если код отработал быстрее
                    current.setAdditionalProperties(Map.of(
                            "status", Map.of("applicationState", Map.of("state", terminalState))
                    ));
                    resOp.patchStatus(current);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}

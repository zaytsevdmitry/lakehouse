package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class K8sSparkOperatorClientUtils {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final ResourceDefinitionContext SPARK_APP_CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup("sparkoperator.k8s.io")
            .withVersion("v1beta2")
            .withKind("SparkApplication")
            .withNamespaced(true)
            .build();

    public KubernetesClient buildKubernetesClient(String masterUrl, String namespace) {
        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
                .withNamespace(namespace)
                .withTrustCerts(true)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }

    // Константы таймаутов для управления жизненным циклом задачи
    private static final long STARTUP_TIMEOUT_MINUTES = 2;
    private static final long BUSINESS_TIMEOUT_HOURS = 3;

    public void submit(KubernetesClient kubernetesClient, String jsonConf) throws TaskFailedException {
        logger.info("Json conf is {}", jsonConf);
        GenericKubernetesResource sparkApp = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .load(new ByteArrayInputStream(jsonConf.getBytes(StandardCharsets.UTF_8)))
                .item();
        logger.info("Json conf loaded");

        String name = sparkApp.getMetadata().getName();
        String namespace = sparkApp.getMetadata().getNamespace();

        AtomicReference<String> finalState = new AtomicReference<>("TIMEOUT");

        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);

        // 1. Превентивное удаление старого ресурса
        try {
            var existing = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                    .inNamespace(namespace)
                    .withName(name);
            if (existing.get() != null) {
                logger.warn("SparkApplication '{}' already exists in namespace '{}'. Deleting old resource...", name, namespace);
                existing.delete();
                TimeUnit.SECONDS.sleep(2);
            }
        } catch (Exception e) {
            logger.warn("Failed to check or delete existing SparkApplication resource before creation", e);
        }

        try {
            // Создаем чистый ресурс в K8s
            kubernetesClient.resource(sparkApp).create();
            logger.info("SparkApplication '{}' successfully created. Initializing watch...", name);

            // Инициализируем watch ПОСЛЕ физического создания ресурса
            try (Watch watch = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                    .inNamespace(namespace)
                    .withName(name)
                    .watch(new Watcher<GenericKubernetesResource>() {
                        @Override
                        public void eventReceived(Action action, GenericKubernetesResource resource) {
                            String state = extractState(resource);
                            logger.info("Job: {}, State: {}", name, state);

                            finalState.set(state);

                            // Сигнал о прохождении этапа инициализации
                            if ("RUNNING".equals(state) || "COMPLETED".equals(state) || "FAILED".equals(state)) {
                                runningLatch.countDown();
                            }

                            // Сигнал о полном завершении
                            if ("COMPLETED".equals(state) || "FAILED".equals(state)) {
                                terminalLatch.countDown();
                            }
                        }

                        @Override
                        public void onClose(WatcherException cause) {
                            if (cause != null) {
                                logger.error("Watch connection closed with error", cause);
                            }
                            runningLatch.countDown();
                            terminalLatch.countDown();
                        }
                    })) {

                // ЭТАП 1: Ожидаем старта задачи максимум 2 минуты
                boolean startedInTime = runningLatch.await(STARTUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                if (!startedInTime) {
                    logger.warn("Task '{}' failed to start within {} minutes.", name, STARTUP_TIMEOUT_MINUTES);
                    throw new TaskFailedException("Spark job submission timed out during startup.");
                }

                // ЭТАП 2: Переходим к длительному бизнес-ожиданию выполнения (3 часа)
                String currentState = finalState.get();
                if ("RUNNING".equals(currentState)) {
                    logger.info("Task '{}' is RUNNING. Waiting up to {} hours for completion...", name, BUSINESS_TIMEOUT_HOURS);

                    boolean completedInTime = terminalLatch.await(BUSINESS_TIMEOUT_HOURS, TimeUnit.HOURS);
                    if (!completedInTime) {
                        logger.error("Task '{}' exceeded the maximum business execution limit of {} hours.", name, BUSINESS_TIMEOUT_HOURS);
                        finalState.set("TIMEOUT"); // Принудительно выставляем таймаут для логики очистки ресурса
                        throw new TaskFailedException("Spark job execution hit the business timeout limit.");
                    }
                } else {
                    logger.info("Task '{}' bypassed RUNNING state and went directly to: {}", name, currentState);
                }
            }

            // Проверяем финальный статус выполнения
            String stateResult = finalState.get();
            if ("UNKNOWN".equals(stateResult) || "SUBMITTED".equals(stateResult)) {
                throw new TaskFailedException("SparkApplication stopped or connection broke in intermediate state: " + stateResult);
            }

        } catch (TaskFailedException e) {
                throw e;
        } catch (Exception e) {
            throw new TaskFailedException("Error during Spark job submission or monitoring", e);
        } finally {
            String currentState = finalState.get();
            logger.info("Final state evaluated as: {}", currentState);
            logDelivery(kubernetesClient, sparkApp);
            // Удаляем ресурс ТОЛЬКО если задача завершилась успешно (COMPLETED)
            if ("COMPLETED".equals(currentState)) {
                try {
                    logger.info("Cleaning up successful SparkApplication resource: {}", name);
                    kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                            .inNamespace(namespace)
                            .withName(name)
                            .delete();
                } catch (Exception e) {
                    logger.error("Failed to clean up successful SparkApplication resource", e);
                }
            } else {
                logger.warn("SparkApplication '{}' was NOT deleted from K8s cluster because its status is '{}'. Inspect it manually.", name, currentState);
            }
        }

        // Бросаем исключение наружу, если задача завершилась не успешно
        if ("FAILED".equals(finalState.get())) {
            throw new TaskFailedException("Spark job execution failed in Kubernetes.");
        } else if ("TIMEOUT".equals(finalState.get())) {
            throw new TaskFailedException("Spark job execution timed out.");
        }
    }


    private String extractState(GenericKubernetesResource resource) {
        try {
            if (resource == null
                    || resource.getAdditionalProperties() == null
                    || !resource.getAdditionalProperties().containsKey("status")
                    ||  resource.getAdditionalProperties().get("status") == null
                    || !((Map<String, Object>) resource.getAdditionalProperties().get("status")).containsKey("applicationState")) {
                return "UNKNOWN";
            }
            Map<String, Object> status = (Map<String, Object>) resource.getAdditionalProperties().get("status");
            Map<String, Object> appState = (Map<String, Object>) status.get("applicationState");
            return (String) appState.get("state");

        } catch (Exception e) {
            logger.debug("Failed to extract state: {}", e.getLocalizedMessage());
            return "UNKNOWN";
        }
    }
    private void logDelivery(KubernetesClient kubernetesClient, GenericKubernetesResource sparkApp){


        List<Pod> driverPods = kubernetesClient.pods()
                .inNamespace(sparkApp.getMetadata().getNamespace())
                .withLabel("sparkoperator.k8s.io/app-name", sparkApp.getMetadata().getName())
                .withLabel("spark-role", "driver")
                .list()
                .getItems();

        logger.info("Count of driver pods for logging: {} ", driverPods.size());

        List<Pod> executorPods = kubernetesClient.pods()
                .inNamespace(sparkApp.getMetadata().getNamespace())
                .withLabel("spark-role", "executor")
                .withLabel("sparkoperator.k8s.io/app-name", sparkApp.getMetadata().getName())
                .list()
                .getItems();

        logger.info("Count of excutor pods for logging: {} ", executorPods.size());

        List<Pod> allPods = new ArrayList<>(driverPods);
        allPods.addAll(executorPods);

        for (Pod pod : allPods) {
            String podName = pod.getMetadata().getName();
            logger.info("Getting logs for pod: {}", podName);

            String logs = kubernetesClient.pods()
                    .inNamespace(sparkApp.getMetadata().getNamespace())
                    .withName(podName)
                    .getLog();
            logger.info("--- Logs from pod {} ---------------\n", podName);
            logger.info(logs);
            logger.info("----End of logs from pod {} ---------\n", podName);
        }
    }
}

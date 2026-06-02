package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class K8sSparkOperatorClientUtils {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String GROUP = "sparkoperator.k8s.io";
    private final static String VERSION = "v1beta2";
    private final static String KIND = "SparkApplication";
    private final static ResourceDefinitionContext SPARK_APP_CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup(GROUP)
            .withVersion(VERSION)
            .withKind(KIND)
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

    public void submit(
            KubernetesClient kubernetesClient,
            String jsonConf,
            long startupTimeoutMinutes,
            long businessTimeoutMinutes
    ) throws TaskFailedException {
        logger.info("Json conf is {}", jsonConf);

        GenericKubernetesResource sparkApp = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .load(new ByteArrayInputStream(jsonConf.getBytes(StandardCharsets.UTF_8)))
                .item();

        logger.info("Json conf loaded");

        cleanUpResource(kubernetesClient,sparkApp);

        try {
            createResource(
                    kubernetesClient,
                    sparkApp,
                    startupTimeoutMinutes,
                    businessTimeoutMinutes
            );

        } catch (TaskFailedException e) {
                throw e;
        } catch (Exception e) {
            throw new TaskFailedException("Error during Spark job submission or monitoring", e);
        } finally {
            logDelivery(kubernetesClient, sparkApp);
            cleanUpResource(kubernetesClient,sparkApp);
        }

    }

    private void cleanUpResource(
            KubernetesClient kubernetesClient,
            GenericKubernetesResource sparkApp
            ){
        String namespace = sparkApp.getMetadata().getNamespace();
        String name      = sparkApp.getMetadata().getName();

        try {
            logger.info("Cleaning up pods for SparkApplication '{}' in namespace '{}'...", name, namespace);
            kubernetesClient.pods()
                    .inNamespace(namespace)
                    .withLabel("sparkoperator.k8s.io/app-name", name)
                    .delete();
        } catch (Exception e) {
            logger.warn("Failed to delete pods for SparkApplication '{}'", name, e);
        }
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
    }

    private void createResource(
            KubernetesClient kubernetesClient,
            GenericKubernetesResource sparkApp,
            Long startupTimeoutMinutes,
            Long businessTimeoutMinutes
    ) throws TaskFailedException, InterruptedException {
        String namespace = sparkApp.getMetadata().getNamespace();
        String name      = sparkApp.getMetadata().getName();
        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);

        AtomicReference<PodState> finalState = new AtomicReference<>(PodState.TIMEOUT);

        kubernetesClient.resource(sparkApp).create();
        logger.info("SparkApplication '{}' successfully created. Initializing watch...", name);

        // Инициализируем watch ПОСЛЕ физического создания ресурса
        try (Watch watch = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .inNamespace(namespace)
                .withName(name)
                    .watch(new K8sSparkOperatorResourceWatcher(name,finalState, runningLatch, terminalLatch))) {

            // Stage 1: waiting for run. Limit 2 minutes
            boolean startedInTime = runningLatch.await(startupTimeoutMinutes, TimeUnit.MINUTES);
            if (!startedInTime) {
                logger.warn("Task '{}' failed to start within {} minutes.", name, startupTimeoutMinutes);
                throw new TaskFailedException("Spark job submission timed out during startup.");
            }

            // Stage 2: Wait for complete. Limit 3 hours
            PodState currentState = finalState.get();
            if (PodState.RUNNING.equals(currentState)) {
                logger.info("Task '{}' is RUNNING. Waiting up to {} minutes for completion...", name, businessTimeoutMinutes);

                boolean completedInTime = terminalLatch.await(businessTimeoutMinutes, TimeUnit.MINUTES);
                if (!completedInTime) {
                    logger.error("Task '{}' exceeded the maximum business execution limit of {} minutes.", name, businessTimeoutMinutes);
                    finalState.set(PodState.TIMEOUT); // Принудительно выставляем таймаут для логики очистки ресурса
                    throw new TaskFailedException("Spark job execution hit the business timeout limit.");
                }
            } else {
                logger.info("Task '{}' bypassed RUNNING state and went directly to: {}", name, currentState);
            }
        }

        PodState stateResult = finalState.get();

        logger.info("Final state evaluated as: {}", finalState.get());
        if (PodState.UNKNOWN.equals(stateResult) || PodState.SUBMITTED.equals(stateResult)) {
            throw new TaskFailedException("SparkApplication stopped or connection broke in intermediate state: " + stateResult);
        }
        else if (PodState.FAILED.equals(finalState.get())) {
            throw new TaskFailedException("Spark job execution failed in Kubernetes.");
        }
        else if (PodState.TIMEOUT.equals(finalState.get())) {
            throw new TaskFailedException("Spark job execution timed out.");
        }
    }

    private PodState extractState(GenericKubernetesResource resource) {
        try {
            if (resource == null
                    || resource.getAdditionalProperties() == null
                    || !resource.getAdditionalProperties().containsKey("status")
                    ||  resource.getAdditionalProperties().get("status") == null
                    || !((Map<String, Object>) resource.getAdditionalProperties().get("status")).containsKey("applicationState")) {
                return PodState.UNKNOWN;
            }
            Map<String, Object> status = (Map<String, Object>) resource.getAdditionalProperties().get("status");
            Map<String, Object> appState = (Map<String, Object>) status.get("applicationState");
            return  PodState.valueOf(appState.get("state").toString());

        } catch (Exception e) {
            logger.debug("Failed to extract state: {}", e.getLocalizedMessage());
            return PodState.UNKNOWN;
        }
    }
    private void logDelivery(KubernetesClient kubernetesClient, GenericKubernetesResource sparkApp){

        try {

            List<Pod> driverPods = kubernetesClient.pods()
                    .inNamespace(sparkApp.getMetadata().getNamespace())
                    .withLabel(GROUP.concat("/app-name"), sparkApp.getMetadata().getName())
                    .withLabel("spark-role", "driver")
                    .list()
                    .getItems();

            logger.info("Count of driver pods for logging: {} ", driverPods.size());

            for (Pod pod : driverPods) {
                String podName = pod.getMetadata().getName();
                logger.info("Getting logs for pod: {}", podName);
                try (InputStream is = kubernetesClient.pods()
                        .inNamespace(sparkApp.getMetadata().getNamespace())
                        .withName(podName)
                        .watchLog()
                        .getOutput();
                     InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     BufferedReader reader = new BufferedReader(isr, 8192)) {
                    String line;
                    long lineCount = 0;
                    logger.info("--- Logs from pod {} ---------------\n", podName);

                    while ((line = reader.readLine()) != null) {
                        logger.info("[{}]: {}", podName, line);
                        lineCount++;
                    }

                    logger.info("----End of logs from pod {} ---------\n", podName);
                } catch (KubernetesClientException | IOException e) {
                    logger.info(e.getLocalizedMessage());
                    logger.debug(String.valueOf(e.fillInStackTrace()));
                }
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }
}

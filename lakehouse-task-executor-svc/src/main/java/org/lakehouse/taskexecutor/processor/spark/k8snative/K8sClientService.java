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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.*;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class K8sClientService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
            long businessTimeoutMinutes,
            boolean cleanUpIfFail,
            boolean logDeliveryIfFail
    ) throws TaskFailedException {
        logger.info("Json conf is {}", jsonConf);

        Pod pod = kubernetesClient.pods().load(new ByteArrayInputStream(jsonConf.getBytes(StandardCharsets.UTF_8))).item();

        logger.info("Json conf loaded");

        String namespace = pod.getMetadata().getNamespace();
        String name = pod.getMetadata().getName();

        cleanUpResource(kubernetesClient, namespace, name);
        boolean isFailed = false;
        try {
            createResource(kubernetesClient, pod);
            watchForSuccess(kubernetesClient,namespace,name,startupTimeoutMinutes,businessTimeoutMinutes);
        } catch (TaskFailedException e) {
            isFailed = true;
            throw e;
        } catch (Exception e) {
            isFailed = true;
            throw new TaskFailedException("Error during Spark job submission or monitoring", e);
        } finally {
            if (isFailed && logDeliveryIfFail)
                logDelivery(kubernetesClient, namespace, name);

            boolean skipCleanup = isFailed && !cleanUpIfFail;
            if (!skipCleanup)
                cleanUpResource(kubernetesClient, namespace, name);
        }
    }
    protected void createResource(
            KubernetesClient kubernetesClient,
            Pod pod
    ) throws TaskFailedException {
        String namespace = pod.getMetadata().getNamespace();
        String name = pod.getMetadata().getName();

        kubernetesClient.pods().inNamespace(namespace).resource(pod).create();
        logger.info("Spark Driver Pod '{}' successfully created. Registering watch...", name);
    }

    // todo made public fot outside watching long running tasks
    protected void watchForSuccess(
            KubernetesClient kubernetesClient,
            String namespace,
            String name,
            long startupTimeoutMinutes,
            long businessTimeoutMinutes) throws TaskFailedException{
        CountDownLatch runningLatch = new CountDownLatch(1);
        CountDownLatch terminalLatch = new CountDownLatch(1);
        AtomicReference<PodPhase> finalState = new AtomicReference<>(PodPhase.TIMEOUT);

        try (Watch watch = kubernetesClient.pods()
                .inNamespace(namespace)
                .withName(name)
                .watch(new K8sPodStatusWatcher(name, finalState, runningLatch, terminalLatch))) {

            boolean startedInTime = runningLatch.await(startupTimeoutMinutes, TimeUnit.MINUTES);
            if (!startedInTime) {
                logger.warn("Task Pod '{}' failed to start within {} minutes.", name, startupTimeoutMinutes);
                throw new TaskFailedException("Spark job submission timed out during startup.");
            }

            PodPhase currentState = finalState.get();
            if (PodPhase.RUNNING.equals(currentState) || PodPhase.PENDING.equals(currentState)) {
                logger.info("Task Pod '{}' is active. Waiting up to {} minutes for completion...", name, businessTimeoutMinutes);
                boolean completedInTime = terminalLatch.await(businessTimeoutMinutes, TimeUnit.MINUTES);
                if (!completedInTime) {
                    logger.error("Task Pod '{}' exceeded execution limit of {} minutes.", name, businessTimeoutMinutes);
                    finalState.set(PodPhase.TIMEOUT);
                    throw new TaskFailedException("Spark job execution hit the business timeout limit.");
                }
            } else {
                logger.info("Task Pod '{}' bypassed active states and went directly to: {}", name, currentState);
            }
        } catch (InterruptedException e) {
            throw new TaskFailedException( "Thread interrupted" ,e);
        }
        PodPhase stateResult = finalState.get();
        logger.info("Final Pod phase evaluated as: {}", stateResult);

        if (PodPhase.UNKNOWN.equals(stateResult) || PodPhase.SUBMITTED.equals(stateResult)) {
            throw new TaskFailedException("Spark Driver Pod stopped unexpectedly or connection broke in phase: " + stateResult);
        } else if (PodPhase.FAILED.equals(stateResult)) {
            throw new TaskFailedException("Spark job execution failed in Kubernetes Pod.");
        } else if (PodPhase.TIMEOUT.equals(stateResult)) {
            throw new TaskFailedException("Spark job execution timed out.");
        }
    }


    protected void cleanUpResource(KubernetesClient kubernetesClient, String namespace, String name) {
        try {
            logger.info("Cleaning up Pod '{}' in namespace '{}'...", name, namespace);
            kubernetesClient.pods().inNamespace(namespace).withName(name).delete();
        } catch (Exception e) {
            logger.error("Failed to delete Pod '{}' during cleanup", name, e);
        }
    }

    protected void logDelivery(KubernetesClient kubernetesClient, String namespace, String name) {
        logger.info("Fetching logs for Spark Driver Pod '{}'...", name);
        try {
            String logs = kubernetesClient.pods().inNamespace(namespace).withName(name).getLog();
            if (logs != null && !logs.isEmpty()) {
                logger.info("\n--- SPARK DRIVER LOGS START ---\n{}\n--- SPARK DRIVER LOGS END ---", logs);
            } else {
                logger.warn("Logs for Pod '{}' are empty.", name);
            }
        } catch (Exception e) {
            logger.error("Could not retrieve logs for Pod '{}'. The pod might have been terminated too fast or misconfigured.", name, e);
        }
    }
}


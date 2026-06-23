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
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class K8sPodStatusWatcher implements Watcher<Pod> {
    private static final Logger logger = LoggerFactory.getLogger(K8sPodStatusWatcher.class);

    private final String podName;
    private final AtomicReference<PodPhase> finalState;
    private final CountDownLatch runningLatch;
    private final CountDownLatch terminalLatch;

    public K8sPodStatusWatcher(String podName, 
                                AtomicReference<PodPhase> finalState, 
                                CountDownLatch runningLatch, 
                                CountDownLatch terminalLatch) {
        this.podName = podName;
        this.finalState = finalState;
        this.runningLatch = runningLatch;
        this.terminalLatch = terminalLatch;
    }

    @Override
    public void eventReceived(Action action, Pod pod) {
        if (pod == null || pod.getStatus() == null) {
            return;
        }
        String phase = pod.getStatus().getPhase(); // Например: Pending, Running, Succeeded, Failed
        logger.debug("Received event {} for Pod '{}', phase: {}", action, podName, phase);

        switch (phase) {
            case "Pending":
                logger.info("Pod '{}' state {}.", podName, PodPhase.PENDING);
                finalState.set(PodPhase.PENDING);
                break;

            case "Running":
                logger.info("Pod '{}' state {}.", podName, PodPhase.RUNNING);
                finalState.set(PodPhase.RUNNING);
                runningLatch.countDown(); // Сигнализируем, что под успешно запустился
                break;

            case "Succeeded":
                logger.info("Pod '{}' completed successfully (Succeeded).", podName);
                finalState.set(PodPhase.SUCCEEDED); // Предполагается, что COMPLETED есть в вашем enum PodPhase
                runningLatch.countDown();  // На случай, если проскочил RUNNING
                terminalLatch.countDown(); // Сигнализируем о завершении работы
                break;

            case "Failed":
                logger.error("Pod '{}' failed.", podName);
                finalState.set(PodPhase.FAILED);
                runningLatch.countDown();
                terminalLatch.countDown();
                break;

            case "Unknown":
            default:
                logger.info("Pod '{}' state {}.", podName, PodPhase.UNKNOWN);
                finalState.set(PodPhase.UNKNOWN);
                break;
        }
    }

    @Override
    public void onClose(WatcherException cause) {
        if (cause != null) {
            logger.error("Watch for Pod '{}' closed with error", podName, cause);
            // Unlock
            runningLatch.countDown();
            terminalLatch.countDown();
        } else {
            logger.info("Watch for Pod '{}' closed normally", podName);
        }
    }
}

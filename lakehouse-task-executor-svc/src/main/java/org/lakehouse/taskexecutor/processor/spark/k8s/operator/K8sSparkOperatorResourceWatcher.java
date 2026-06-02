package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
public class K8sSparkOperatorResourceWatcher  implements Watcher<GenericKubernetesResource> {
    private  final Logger logger = LoggerFactory.getLogger(K8sSparkOperatorResourceWatcher.class);

    private final String jobName;
    private final AtomicReference<PodState> finalState;
    private final CountDownLatch runningLatch;
    private final CountDownLatch terminalLatch;


    public K8sSparkOperatorResourceWatcher(
            String jobName,
            AtomicReference<PodState> finalState,
            CountDownLatch runningLatch,
            CountDownLatch terminalLatch) {
        this.jobName = jobName;
        this.finalState = finalState;
        this.runningLatch = runningLatch;
        this.terminalLatch = terminalLatch;
    }

    @Override
    public void eventReceived(Action action, GenericKubernetesResource resource) {
        PodState state = extractState(resource);
        logger.info("Job: {}, State: {}", jobName, state.getValue());

        if (state != PodState.UNKNOWN) {
            finalState.set(state);
        }

        if (state.equals(PodState.RUNNING) || state.equals(PodState.COMPLETED) || state.equals(PodState.FAILED)) {
            runningLatch.countDown();
        }

        if (state.equals(PodState.COMPLETED) || state.equals(PodState.FAILED)) {
            terminalLatch.countDown();
        }
    }

    @Override
    public void onClose(WatcherException cause) {
        if (cause != null) {
            logger.error("Watch connection closed with error for job: {}", jobName, cause);
        }
        runningLatch.countDown();
        terminalLatch.countDown();
    }

    @SuppressWarnings("unchecked")
    private PodState extractState(GenericKubernetesResource resource) {
        try {
            if (resource == null
                    || resource.getAdditionalProperties() == null
                    || !resource.getAdditionalProperties().containsKey("status")
                    || resource.getAdditionalProperties().get("status") == null
                    || !((Map<String, Object>) resource.getAdditionalProperties().get("status")).containsKey("applicationState")) {
                return PodState.UNKNOWN;
            }
            Map<String, Object> status = (Map<String, Object>) resource.getAdditionalProperties().get("status");
            Map<String, Object> appState = (Map<String, Object>) status.get("applicationState");
            if (appState == null) {
                return PodState.UNKNOWN;
            }
            return PodState.fromString((String) appState.get("state"));
        } catch (Exception e) {
            logger.debug("Failed to extract state for {}: {}", jobName, e.getLocalizedMessage());
            return PodState.UNKNOWN;
        }
    }
}

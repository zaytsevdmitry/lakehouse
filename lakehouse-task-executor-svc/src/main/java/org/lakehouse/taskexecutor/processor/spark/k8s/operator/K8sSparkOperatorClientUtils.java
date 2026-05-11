package org.lakehouse.taskexecutor.processor.spark.k8s.operator;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.*;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext; // Пакет для 7.6.1
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

// ...

public class K8sSparkOperatorClientUtils {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // Явно описываем структуру ресурса
    private static final ResourceDefinitionContext SPARK_APP_CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup("sparkoperator.k8s.io")
            .withVersion("v1beta2")
            .withKind("SparkApplication")
            .withNamespaced(true)
            .build();

    public KubernetesClient buildKubernetesClient(
            String masterUrl,
            //String token,
            String namespace) {
        Config config = new ConfigBuilder()
                .withMasterUrl(masterUrl)
              //  .withOauthToken(token)
                .withNamespace(namespace)
                .withTrustCerts(true)
                .build();
        return new KubernetesClientBuilder().withConfig(config).build();
    }
    public void submit(KubernetesClient kubernetesClient, String jsonConf) throws TaskFailedException {
        logger.info("Json conf is {}", jsonConf);
        GenericKubernetesResource sparkApp = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .load(new ByteArrayInputStream(jsonConf.getBytes(StandardCharsets.UTF_8)))
                .item();

        logger.info("Json conf loaded");

        String name = sparkApp.getMetadata().getName();
        String namespace = sparkApp.getMetadata().getNamespace();
        AtomicReference<String> finalState = new AtomicReference<>("TIMEOUT");
        CountDownLatch latch = new CountDownLatch(1);
        try (Watch watch = kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .inNamespace(namespace)
                .withName(name)
                .watch(new Watcher<GenericKubernetesResource>() {
                    @Override
                    public void eventReceived(Action action, GenericKubernetesResource resource) {
                        String state = extractState(resource);
                        logger.info("Job: {}, State: {}", name, state);

                        if ("COMPLETED".equals(state) || "FAILED".equals(state)) {
                            finalState.set(state);
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        if (cause != null) latch.countDown();
                    }
                })) {

            // Для создания можно использовать обычный ресурс
            kubernetesClient.resource(sparkApp).create();
            latch.await(20, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new TaskFailedException(e);
        }

        // Cleanup через контекст
        kubernetesClient.genericKubernetesResources(SPARK_APP_CONTEXT)
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }
    private String extractState(GenericKubernetesResource resource) {
        try {
            // В GenericResource status лежит в AdditionalProperties
            Map<String, Object> status = (Map<String, Object>) resource.getAdditionalProperties().get("status");
            return (String) ((Map<String, Object>) status.get("applicationState")).get("state");
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}

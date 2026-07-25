package org.lakehouse.task.proxy.spark.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import org.lakehouse.task.proxy.spark.adapter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;

@Configuration
public class AdapterConfig {

    private static final Logger log = LoggerFactory.getLogger(AdapterConfig.class);

    @Bean
    public SparkAdapter sparkClusterAdapter(ProxyConfig config) {
        String adapterType = config.getAdapter().toLowerCase();
        log.info("Creating adapter for type: {}", adapterType);

        return switch (adapterType) {
            case "standalone" -> new StandaloneSparkAdapter(config.getSparkMaster(), config.getStandalone().getRestUrl());
            case "k8s", "kubernetes" -> {
                try {
                    String basePath = getBasePath(config.getK8s().getRestUrl());
                    log.info("K8s API base path: {}", basePath);
                    ApiClient apiClient = ClientBuilder.standard().setBasePath(basePath).build();
                    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
                    yield new KubernetesSparkAdapter(config.getSparkMaster(), coreV1Api, config.getK8s().getNamespace());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to initialize Kubernetes client", e);
                }
            }
            case "yarn" -> new YarnSparkAdapter(config.getSparkMaster(), config.getYarn().getRestUrl());
            case "mesos" -> new MesosSparkAdapter(config.getSparkMaster());
            default -> throw new IllegalArgumentException("Unsupported adapter type: " + adapterType);
        };
    }

    private String getBasePath(String restUrl) {
        URI uri = URI.create(restUrl);
        String scheme = uri.getScheme() != null ? uri.getScheme() : "http";
        String host = uri.getHost() != null ? uri.getHost() : "localhost";
        int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
        return scheme + "://" + host + ":" + port;
    }
}

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
        long timeout = config.getMetrics().getSubmissionTimeoutSeconds();
        log.info("Creating adapter for type: {}, submissionTimeoutSeconds={}", adapterType, timeout);

        return switch (adapterType) {
            case "standalone" -> new StandaloneSparkAdapter(config.getSparkMaster(), config.getStandalone().getRestUrl(), timeout,
                    config.getStandalone().getSubmissionIdPattern());
            case "k8s", "kubernetes" -> {
                try {
                    String basePath = getBasePath(config.getK8s().getRestUrl());
                    log.info("K8s API base path: {}", basePath);
                    ApiClient apiClient = ClientBuilder.standard().setBasePath(basePath).build();
                    CoreV1Api coreV1Api = new CoreV1Api(apiClient);
                    yield new KubernetesSparkAdapter(config.getSparkMaster(), coreV1Api, config.getK8s().getNamespace(), timeout,
                            config.getK8s().getSubmissionIdPattern());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to initialize Kubernetes client", e);
                }
            }
            case "yarn" -> new YarnSparkAdapter(config.getSparkMaster(), config.getYarn().getRestUrl(), timeout,
                    config.getYarn().getSubmissionIdPattern());
            case "mesos" -> new MesosSparkAdapter(config.getSparkMaster(), timeout);
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

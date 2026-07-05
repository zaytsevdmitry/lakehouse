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
package org.lakehouse.taskexecutor.processor.spark.k8snative.mappers;

import io.fabric8.kubernetes.api.model.*;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class ContainerMapperService {

    // Public constants for container configuration properties
    public static final String CONTAINER_NAME = "name";
    public static final String CONTAINER_IMAGE = "image";
    public static final String CONTAINER_IMAGE_PULL_POLICY = "imagePullPolicy";
    public static final String CONTAINER_COMMAND = "command";
    public static final String CONTAINER_ARGS = "args";

    // Nested prefixes
    public static final String CONTAINER_ENV_PREFIX = "env.";
    public static final String CONTAINER_RESOURCES_PREFIX = "resources.";
    public static final String RESOURCE_LIMITS_PREFIX = "limits.";
    public static final String RESOURCE_REQUESTS_PREFIX = "requests.";

    /**
     * Builds a single Container object from a clean configuration map.
     * Expects keys without container index prefixes, e.g., "name", "image", "env.DB_HOST".
     */
    public  Container fillContainer(Map<String, String> containerConf) {
        ContainerBuilder containerBuilder = new ContainerBuilder();

        // 1. Base Fields
        containerBuilder.withName(containerConf.getOrDefault(CONTAINER_NAME, "app-container"));
        containerBuilder.withImage(containerConf.getOrDefault(CONTAINER_IMAGE, null));
        containerBuilder.withImagePullPolicy(containerConf.getOrDefault(CONTAINER_IMAGE_PULL_POLICY, null));

        // 2. Command & Args Parsing
        String commandStr = containerConf.getOrDefault(CONTAINER_COMMAND, null);
        if (commandStr != null && !commandStr.isBlank()) {
            containerBuilder.withCommand(Arrays.stream(commandStr.split(","))
                    .map(String::trim)
                    .toList());
        }

        String argsStr = containerConf.getOrDefault(CONTAINER_ARGS, null);
        if (argsStr != null && !argsStr.isBlank()) {
            containerBuilder.withArgs(Arrays.stream(argsStr.split(","))
                    .map(String::trim)
                    .toList());
        }

        // 3. Environment Variables Mapping (Using ConfUtil.extractConf)
        fillContainerEnv(containerBuilder, containerConf);

        // 4. Resources Mapping (Using ConfUtil.extractConf)
        fillContainerResources(containerBuilder, containerConf);

        return containerBuilder.build();
    }

    /**
     * Extracts keys starting with "env." and maps them as environment variables.
     * Input example: "env.DB_URL=jdbc:..." -> Extracted: "DB_URL=jdbc:..."
     */
    private  void fillContainerEnv(ContainerBuilder containerBuilder, Map<String, String> containerConf) {
        Map<String, String> envMap = ConfUtil.extractConf(containerConf, CONTAINER_ENV_PREFIX);
        if (!envMap.isEmpty()) {
            List<EnvVar> envVars = new ArrayList<>();
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                envVars.add(new EnvVarBuilder()
                        .withName(entry.getKey())
                        .withValue(entry.getValue())
                        .build());
            }
            containerBuilder.withEnv(envVars);
        }
    }

    /**
     * Extracts keys starting with "resources." and maps requests and limits.
     * Input example: "resources.limits.cpu=500m" -> Extracted to resource requirements
     */
    private  void fillContainerResources(ContainerBuilder containerBuilder, Map<String, String> containerConf) {
        // Step 1: Strip "resources." prefix. Keys become: "limits.cpu", "requests.memory", etc.
        Map<String, String> resourcesConf = ConfUtil.extractConf(containerConf, CONTAINER_RESOURCES_PREFIX);
        if (resourcesConf.isEmpty()) {
            return;
        }

        ResourceRequirementsBuilder resourceBuilder = new ResourceRequirementsBuilder();

        // Step 2: Extract "limits." from the resources sub-map. Keys become: "cpu", "memory"
        Map<String, String> limitsConf = ConfUtil.extractConf(resourcesConf, RESOURCE_LIMITS_PREFIX);
        if (!limitsConf.isEmpty()) {
            Map<String, Quantity> limits = new HashMap<>();
            limitsConf.forEach((resourceName, value) -> limits.put(resourceName, new Quantity(value)));
            resourceBuilder.withLimits(limits);
        }

        // Step 3: Extract "requests." from the resources sub-map. Keys become: "cpu", "memory"
        Map<String, String> requestsConf = ConfUtil.extractConf(resourcesConf, RESOURCE_REQUESTS_PREFIX);
        if (!requestsConf.isEmpty()) {
            Map<String, Quantity> requests = new HashMap<>();
            requestsConf.forEach((resourceName, value) -> requests.put(resourceName, new Quantity(value)));
            resourceBuilder.withRequests(requests);
        }

        containerBuilder.withResources(resourceBuilder.build());
    }
}

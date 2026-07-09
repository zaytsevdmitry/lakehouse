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
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PodSpecMapperService {

    // Prefixes for complex structures
    public static final String SPEC_VOLUMES_PREFIX = "spec.volumes.";
    public static final String SPEC_CONTAINERS_PREFIX = "spec.containers.";

    // Public constants for simple fields mapping
    public static final String SPEC_RESTART_POLICY = "spec.restartPolicy";
    public static final String SPEC_SERVICE_ACCOUNT_NAME = "spec.serviceAccountName";
    public static final String SPEC_NODE_NAME = "spec.nodeName";
    public static final String SPEC_SUBDOMAIN = "spec.subdomain";
    public static final String SPEC_HOSTNAME = "spec.hostname";
    public static final String SPEC_SCHEDULER_NAME = "spec.schedulerName";
    public static final String SPEC_PRIORITY_CLASS_NAME = "spec.priorityClassName";
    public static final String SPEC_RUNTIME_CLASS_NAME = "spec.runtimeClassName";
    public static final String SPEC_TERMINATION_GRACE_PERIOD_SECONDS = "spec.terminationGracePeriodSeconds";
    public static final String SPEC_ACTIVE_DEADLINE_SECONDS = "spec.activeDeadlineSeconds";
    public static final String SPEC_PRIORITY = "spec.priority";
    public static final String SPEC_HOST_NETWORK = "spec.hostNetwork";
    public static final String SPEC_HOST_PID = "spec.hostPID";
    public static final String SPEC_HOST_IPC = "spec.hostIPC";
    public static final String SPEC_SHARE_PROCESS_NAMESPACE = "spec.shareProcessNamespace";
    public static final String SPEC_AUTOMOUNT_SERVICE_ACCOUNT_TOKEN = "spec.automountServiceAccountToken";

    private final ContainerMapperService containerMapperService;
    private final VolumeMapperService volumeMapperService;

    @Autowired
    public PodSpecMapperService(ContainerMapperService containerMapperService, VolumeMapperService volumeMapperService) {
        this.containerMapperService = containerMapperService;
        this.volumeMapperService = volumeMapperService;
    }

    /**
     * Completely populates a PodSpec from a configuration Map using managed services and ConfUtil.
     *
     * @throws TaskConfigurationException if boolean or numeric fields parsing fails
     */
    public PodSpec fillPodSpec(Map<String, String> specConf) throws TaskConfigurationException {
        PodSpecBuilder builder = new PodSpecBuilder();

        // =========================================================================
        // 1. POPULATE SIMPLE FIELDS (Using ConfUtil conversions)
        // =========================================================================
        builder.withRestartPolicy(specConf.getOrDefault(SPEC_RESTART_POLICY, null));
        builder.withServiceAccountName(specConf.getOrDefault(SPEC_SERVICE_ACCOUNT_NAME, null));
        builder.withNodeName(specConf.getOrDefault(SPEC_NODE_NAME, null));
        builder.withSubdomain(specConf.getOrDefault(SPEC_SUBDOMAIN, null));
        builder.withHostname(specConf.getOrDefault(SPEC_HOSTNAME, null));
        builder.withSchedulerName(specConf.getOrDefault(SPEC_SCHEDULER_NAME, null));
        builder.withPriorityClassName(specConf.getOrDefault(SPEC_PRIORITY_CLASS_NAME, null));
        builder.withRuntimeClassName(specConf.getOrDefault(SPEC_RUNTIME_CLASS_NAME, null));

        // Safely parse Null-allowed Long/Integer properties using ConfUtil
        String graceStr = specConf.getOrDefault(SPEC_TERMINATION_GRACE_PERIOD_SECONDS, null);
        builder.withTerminationGracePeriodSeconds(graceStr != null ? ConfUtil.getLongByKey(specConf, SPEC_TERMINATION_GRACE_PERIOD_SECONDS, null) : null);

        String activeDeadlineStr = specConf.getOrDefault(SPEC_ACTIVE_DEADLINE_SECONDS, null);
        builder.withActiveDeadlineSeconds(activeDeadlineStr != null ? ConfUtil.getLongByKey(specConf, SPEC_ACTIVE_DEADLINE_SECONDS, null) : null);

        String priorityStr = specConf.getOrDefault(SPEC_PRIORITY, null);
        builder.withPriority(priorityStr != null ? ConfUtil.getLongByKey(specConf, SPEC_PRIORITY, null).intValue() : null);

        // Safely parse Boolean properties (defaults to false via ConfUtil validation)
        builder.withHostNetwork(ConfUtil.getBooleanByKey(specConf, SPEC_HOST_NETWORK, false));
        builder.withHostPID(ConfUtil.getBooleanByKey(specConf, SPEC_HOST_PID, false));
        builder.withHostIPC(ConfUtil.getBooleanByKey(specConf, SPEC_HOST_IPC, false));
        builder.withShareProcessNamespace(ConfUtil.getBooleanByKey(specConf, SPEC_SHARE_PROCESS_NAMESPACE, false));
        builder.withAutomountServiceAccountToken(ConfUtil.getBooleanByKey(specConf, SPEC_AUTOMOUNT_SERVICE_ACCOUNT_TOKEN, false));

        // =========================================================================
        // 2. CALL METHODS FOR COMPLEX FIELDS (Using ConfUtil.extractConf)
        // =========================================================================
        fillNodeSelector(builder, specConf);
        fillImagePullSecrets(builder, specConf);
        fillTolerations(builder, specConf);
        fillHostAliases(builder, specConf);

        // Delegated structures
        fillContainers(builder, specConf);
        fillVolumes(builder, specConf);

        return builder.build();
    }

    private void fillNodeSelector(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> nodeSelector = ConfUtil.extractConf(specConf, "spec.nodeSelector.");
        if (!nodeSelector.isEmpty()) {
            builder.withNodeSelector(nodeSelector);
        }
    }

    private void fillImagePullSecrets(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> secretsMap = ConfUtil.extractConf(specConf, "spec.imagePullSecrets.");
        if (!secretsMap.isEmpty()) {
            List<LocalObjectReference> secrets = new ArrayList<>();
            for (String secretName : secretsMap.keySet()) {
                secrets.add(new LocalObjectReferenceBuilder().withName(secretName.trim()).build());
            }
            builder.withImagePullSecrets(secrets);
        }
    }

    private void fillTolerations(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> extracted = ConfUtil.extractConf(specConf, "spec.tolerations.");
        if (extracted.isEmpty()) return;

        Map<String, Map<String, String>> groupedTolerations = new TreeMap<>();
        for (Map.Entry<String, String> entry : extracted.entrySet()) {
            String[] parts = entry.getKey().split("\\.", 2);
            if (parts.length == 2) {
                groupedTolerations.computeIfAbsent(parts[0], k -> new HashMap<>()).put(parts[1], entry.getValue());
            }
        }

        List<Toleration> tolerations = new ArrayList<>();
        for (Map<String, String> fields : groupedTolerations.values()) {
            tolerations.add(new TolerationBuilder()
                    .withKey(fields.getOrDefault("key", null))
                    .withOperator(fields.getOrDefault("operator", null))
                    .withValue(fields.getOrDefault("value", null))
                    .withEffect(fields.getOrDefault("effect", null))
                    .build());
        }
        builder.withTolerations(tolerations);
    }

    private void fillHostAliases(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> aliasesMap = ConfUtil.extractConf(specConf, "spec.hostAliases.");
        if (aliasesMap.isEmpty()) return;

        List<HostAlias> hostAliases = new ArrayList<>();
        for (Map.Entry<String, String> entry : aliasesMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                List<String> hostnameList = Arrays.stream(entry.getValue().split(","))
                        .map(String::trim)
                        .toList();
                hostAliases.add(new HostAliasBuilder().withIp(entry.getKey()).withHostnames(hostnameList).build());
            }
        }
        builder.withHostAliases(hostAliases);
    }

    private void fillContainers(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> allContainersConf = ConfUtil.extractConf(specConf, SPEC_CONTAINERS_PREFIX);
        if (allContainersConf.isEmpty()) return;

        SortedSet<String> indexes = new TreeSet<>();
        for (String key : allContainersConf.keySet()) {
            String[] parts = key.split("\\.", 2);
            if (parts.length > 0) indexes.add(parts[0]);
        }

        List<Container> containers = new ArrayList<>();
        for (String index : indexes) {
            Map<String, String> singleContainerConf = ConfUtil.extractConf(allContainersConf, index + ".");
            containers.add(containerMapperService.fillContainer(singleContainerConf));
        }
        builder.withContainers(containers);
    }

    private void fillVolumes(PodSpecBuilder builder, Map<String, String> specConf) {
        Map<String, String> allVolumesConf = ConfUtil.extractConf(specConf, SPEC_VOLUMES_PREFIX);
        if (allVolumesConf.isEmpty()) return;

        SortedSet<String> indexes = new TreeSet<>();
        for (String key : allVolumesConf.keySet()) {
            String[] parts = key.split("\\.", 2);
            if (parts.length > 0) indexes.add(parts[0]);
        }

        List<Volume> volumes = new ArrayList<>();
        for (String index : indexes) {
            Map<String, String> singleVolumeConf = ConfUtil.extractConf(allVolumesConf, index + ".");
            // Delegation to the managed Spring service bean instance
            volumes.add(volumeMapperService.fillVolume(singleVolumeConf));
        }
        builder.withVolumes(volumes);
    }
}

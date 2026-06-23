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

package org.lakehouse.taskexecutor.processor.spark;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.k8snative.K8sClientService;
import org.lakehouse.taskexecutor.processor.spark.k8snative.K8sConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class K8sSparkNativeTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final long STARTUP_TIMEOUT_MINUTES = 2;
    private static final long BUSINESS_TIMEOUT_MINUTES = 180;
    private final K8sConfigService k8SConfigService;
    private final K8sClientService k8sClientService;
    public K8sSparkNativeTaskProcessor(K8sConfigService k8SConfigService, K8sClientService k8sClientService) {
        this.k8SConfigService = k8SConfigService;
        this.k8sClientService = k8sClientService;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils)
            throws TaskFailedException,
                   TaskConfigurationException {

        Map<String, String> taskConf = k8SConfigService.extractK8sConf(sourceConfDTO,scheduledTaskDTO);

        taskConf.forEach((s, s2) -> logger.info("Task parameter {} -> {}", s,s2));

        String masterUrl =  k8SConfigService.extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtils);
        logger.info("MasterUrl is {}", masterUrl);

        String namespace = taskConf.getOrDefault("metadata.namespace", "default");
        logger.info("Namespace is {}", namespace);

        KubernetesClient kubernetesClient = k8sClientService.buildKubernetesClient(masterUrl, namespace);

        String json = k8SConfigService.extractAppConfJson(sourceConfDTO,scheduledTaskDTO);

        long startupTimeoutMinutes = K8sConfigService.getLongByKey(
                K8sConfigService.castToStringMap(taskConf),
                "startupTimeoutMinutes",
                STARTUP_TIMEOUT_MINUTES);

        long businessTimeoutMinutes  = K8sConfigService.getLongByKey(
                K8sConfigService.castToStringMap(taskConf),
                "businessTimeoutMinutes",
                BUSINESS_TIMEOUT_MINUTES);

        boolean cleanUpIfFail = K8sConfigService.getBooleanByKey(
                K8sConfigService.castToStringMap(taskConf),
                "cleanUpIfFail",
                true);

        boolean logDeliveryIfFail =  K8sConfigService.getBooleanByKey(
                K8sConfigService.castToStringMap(taskConf),
                "logDeliveryIfFail",
                true);

        k8sClientService.submit(kubernetesClient, json, startupTimeoutMinutes, businessTimeoutMinutes, cleanUpIfFail, logDeliveryIfFail);
    }
}

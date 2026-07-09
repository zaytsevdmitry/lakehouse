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

package org.lakehouse.taskexecutor.processor.spark.k8snative;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Pod;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.k8snative.manifestkeys.PodKeys;
import org.lakehouse.taskexecutor.processor.spark.k8snative.mappers.PodMapperService;
import org.lakehouse.taskexecutor.processor.spark.k8snative.mappers.SparkDriverContainerMapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class K8sConfigService extends ConfUtil {

    final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PodMapperService podMapperService;
    private final SparkDriverContainerMapperService sparkDriverContainerMapperService;
    private final String restConfUrl;
    private final String restSchedulerUrl;


    public K8sConfigService(
            PodMapperService podMapperService, SparkDriverContainerMapperService sparkDriverContainerMapperService,
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl
    ) {
        this.podMapperService = podMapperService;
        this.sparkDriverContainerMapperService = sparkDriverContainerMapperService;
        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
    }

    public Map<String, String> extractK8sConf(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ) {

        return Coalesce.applyMergeNonNullValuesMap(
                extractConf(sourceConfDTO.getTargetDataSource().getService().getProperties(), TaskProcessorArgKey.K8S_NATIVE_PREFIX),
                extractConf(scheduledTaskDTO.getTaskProcessorArgs(), TaskProcessorArgKey.K8S_NATIVE_PREFIX));

    }

    public String extractMasterUrl(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        DriverDTO driverDTO = sourceConfDTO.getTargetDriver();

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(
                    String.format(
                            "Connection template %s is not present in driver %s",
                            Types.ConnectionType.spark.label,
                            driverDTO.getKeyName()));

        if (!scheduledTaskDTO.getTaskProcessorArgs().containsKey(SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY)) {

            throw new TaskConfigurationException(
                    String.format(
                            "Key '%s' is not present in TaskProcessorArgs %s",
                            SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY,
                            scheduledTaskDTO.buildTaskFullName()));
        }

        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        logger.info("MasterUrl template is {}", template);
        String url = jinJavaUtils.render(template);
        logger.info("MasterUrl is {}", url);

        return url;
    }

    public Pod buildSparkDriverPod(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ) throws TaskConfigurationException {
        Map<String, String> taskConf = extractK8sConf(sourceConfDTO, scheduledTaskDTO);
        Map<String, String> manifestConf = extractConf(taskConf, TaskProcessorArgKey.K8S_NATIVE_MANIFEST);
        Map<String, String> sparkConf = SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO);
        Map<String, String> appConf = Coalesce.applyMergeNonNullValuesMap(
                        sourceConfDTO.getTargetDataSource().getService().getProperties(),
                        scheduledTaskDTO.getTaskProcessorArgs())
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.K8S_NATIVE_PREFIX))
                .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.SPARK_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        appConf.put(ConfigRestClientConstants.restConfKey, restConfUrl);
        appConf.put(SchedulerRestClientConstants.restSchedulerKey, restSchedulerUrl);
        loadDefaults(manifestConf, sparkConf, appConf, scheduledTaskDTO);
        sparkTok8sFix(manifestConf, sparkConf, scheduledTaskDTO);

        //render configs
        JinJavaUtils jinJavaUtils = null;
        try {
            jinJavaUtils = JinJavaFactory.getJinJavaUtils(sourceConfDTO, scheduledTaskDTO);
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        renderConfigs(Arrays.asList(taskConf,sparkConf,appConf,manifestConf),jinJavaUtils);

        Pod result = podMapperService.buildPod(manifestConf);
        sparkDriverContainerMapperService
                .applySparkDriverContainer(
                        result,
                        taskConf,
                        sparkConf,
                        appConf,
                        extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtils));
        return result;
    }
    private void renderConfigs(List<Map<String,String>> confs, JinJavaUtils jinJavaUtils){
        for(Map<String,String> m: confs){
            m.putAll(jinJavaUtils.renderMapValues(m));
        }
    }

    private void loadDefaults(
            Map<String, String> manifestConf,
            Map<String, String> sparkConf,
            Map<String, String> appConf,
            ScheduledTaskDTO scheduledTaskDTO) {
        manifestConf.put(
                PodKeys.METADATA_ANNOTATIONS + "." + PodKeys.METADATA_ANNOTATIONS_TASK_NAME_KEY,
                manifestConf.getOrDefault(
                        PodKeys.METADATA_ANNOTATIONS + ".lakehouse-management-task",
                        scheduledTaskDTO.buildTaskFullName()));
        String podMetadataName = getFixedPodName(scheduledTaskDTO, manifestConf.getOrDefault("metadata.name", null));
        manifestConf.put("metadata.name", podMetadataName);
        manifestConf.put(PodKeys.SPEC_RESTART_POLICY, manifestConf.getOrDefault(PodKeys.SPEC_RESTART_POLICY, "never"));
        sparkConf.put("spark.kubernetes.executor.podNamePrefix", podMetadataName);
        appConf.put("scheduledTaskId", String.valueOf(scheduledTaskDTO.getId()));
    }


    /**
     * <p>
     * Settings passed via spark.kubernetes.* parameters that are required during
     * the pre-launch phase of the spark context must be moved to the manifest.
     * These settings only need to be specified in one place: either in the manifest
     * or in spark.kubernetes.* . Specifying them in both places will overwrite the original manifest
     * setting.
     * https://spark.apache.org/docs/3.5.8/running-on-kubernetes.html
     * <p>
     * spark.kubernetes.driver.pod.name == allways rewrite
     * Executor properties leave as is
     * spark.kubernetes.{driver,executor}.label.*
     * spark.kubernetes.{driver,executor}.annotation.*
     * spark.kubernetes.{driver,executor}.volumes.[VolumeType].[VolumeName].mount.path
     * </p>
     */
    private void sparkTok8sFix(
            Map<String, String> manifestConf,
            Map<String, String> sparkConf,
            ScheduledTaskDTO scheduledTaskDTO) throws TaskConfigurationException {

        manifestConf.put(PodKeys.METADATA_NAMESPACE, compareAndGet(
                PodKeys.METADATA_NAMESPACE,
                manifestConf,
                "spark.kubernetes.namespace",
                sparkConf
        ).orElse("default"));

        compareAndGet(
                PodKeys.SPEC_SERVICE_ACCOUNT_NAME,
                manifestConf,
                "spark.kubernetes.authenticate.driver.serviceAccountName",
                sparkConf
        ).ifPresent(s -> manifestConf.put(PodKeys.SPEC_SERVICE_ACCOUNT_NAME, s));

        compareAndGet(
                PodKeys.SPEC_IMAGE_PULL_SECRETS,
                manifestConf,
                "spark.kubernetes.container.image.pullSecrets",
                sparkConf
        ).ifPresent(s -> manifestConf.put(PodKeys.SPEC_IMAGE_PULL_SECRETS, s));

    }

    private Optional<String> compareAndGet(
            String manifestKey,
            Map<String, String> manifestConf,
            String sparkKey,
            Map<String, String> sparkConf) throws TaskConfigurationException {
        String manifestValue = manifestConf.getOrDefault(manifestKey, null);
        String sparkK8sValue = sparkConf.getOrDefault(sparkKey, null);

        if (StringUtils.hasText(manifestValue) && StringUtils.hasText(sparkK8sValue)) {
            if (!Objects.equals(manifestValue, sparkK8sValue)) {
                throw new TaskConfigurationException(
                        String.format(
                                "\"%s\" and \"%s\" contains different values. Use any one of keys or the same values for both key", manifestKey,sparkKey));
            }
        }
        return Optional.ofNullable(Coalesce.apply(manifestValue, sparkK8sValue));
    }

    /**
     * //DNS RFC 1123  Kubernetes
     * //todo This value needs some work for Kubernetes. Additionally, it is expected that the subname cannot exceed 63 characters.
     * //todo next time need create getTaskFullName function in jijava  ".metadata.name" : {{ k8s_dns_rfc_1123(scheduledTask)}}
     */
    String getFixedPodName(
            ScheduledTaskDTO scheduledTaskDTO,
            String name) {
        String rawName = null;
        if (name == null || name.isBlank()) {
            rawName = String.format("task-%d-%d-%d", scheduledTaskDTO.getId(), scheduledTaskDTO.getTryNum(), DateTimeUtils.now().hashCode());
        } else {
            rawName = name;
        }
        String cleanName = rawName.replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-");

        int targetLength = Math.min(cleanName.length(), 63);
        String fixedTaskName = cleanName.substring(0, targetLength);

        if (fixedTaskName.endsWith("-")) {
            fixedTaskName = fixedTaskName.substring(0, fixedTaskName.length() - 1);
        }
        if (fixedTaskName.startsWith("-")) {
            fixedTaskName = fixedTaskName.substring(1);
        }
        return fixedTaskName;
    }

}

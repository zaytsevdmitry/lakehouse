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

    import com.typesafe.config.ConfigFactory;
    import com.typesafe.config.ConfigRenderOptions;
    import io.fabric8.kubernetes.api.model.*;
    import org.lakehouse.client.api.constant.SystemVarKeys;
    import org.lakehouse.client.api.constant.TaskProcessorArgKey;
    import org.lakehouse.client.api.constant.Types;
    import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
    import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
    import org.lakehouse.client.api.dto.task.SourceConfDTO;
    import org.lakehouse.client.api.exception.TaskConfigurationException;
    import org.lakehouse.client.api.utils.Coalesce;
    import org.lakehouse.client.api.utils.ObjectMapping;
    import org.lakehouse.client.api.utils.conf.ConfUtil;
    import org.lakehouse.client.api.utils.conf.SparkConfUtil;
    import org.lakehouse.jinja.java.JinJavaFactory;
    import org.lakehouse.jinja.java.JinJavaUtils;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Service;

    import java.io.IOException;
    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    public class K8sConfigService extends ConfUtil {

        final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final PodUtilService podUtilService;

        public K8sConfigService(PodUtilService podUtilService) {
            this.podUtilService = podUtilService;
        }

        public Map<String, String> extractK8sConf(
                SourceConfDTO sourceConfDTO,
                ScheduledTaskDTO scheduledTaskDTO
        ) {

            return Coalesce.applyMergeNonNullValuesMap(
                    extractConf(sourceConfDTO.getTargetDataSource().getService().getProperties(), TaskProcessorArgKey.K8S_NATIVE),
                    extractConf(scheduledTaskDTO.getTaskProcessorArgs(), TaskProcessorArgKey.K8S_NATIVE));

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
        public String extractAppConfJson(
                SourceConfDTO sourceConfDTO,
                ScheduledTaskDTO scheduledTaskDTO
        ) throws TaskConfigurationException {
            try {

                Map<String,String> taskConf = extractK8sConf(sourceConfDTO, scheduledTaskDTO);
                Map<String, String> manifestConf = extractConf(taskConf, TaskProcessorArgKey.K8S_NATIVE_MANIFEST);
                Map<String, String> sparkConf = SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO);
                Map<String, String> appConf =  Coalesce.applyMergeNonNullValuesMap(
                                sourceConfDTO.getTargetDataSource().getService().getProperties(),
                                scheduledTaskDTO.getTaskProcessorArgs())
                        .entrySet()
                        .stream()
                        .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.K8S_NATIVE))
                        .filter(e -> !e.getKey().startsWith(TaskProcessorArgKey.SPARK_PREFIX))
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
                ConfigRenderOptions options = ConfigRenderOptions.defaults()
                        .setJson(true)
                        .setOriginComments(false)
                        .setComments(false)
                        .setFormatted(true);

                Pod pod = ObjectMapping
                        .stringToObject(
                                ConfigFactory
                                        .parseMap(manifestConf)
                                        .root()
                                        .render(options),
                                Pod.class);


                JinJavaUtils jinJavaUtils = new JinJavaFactory().getJinJavaUtils(sourceConfDTO, scheduledTaskDTO);
                podUtilService.fixDriverPod(pod, taskConf,sparkConf, appConf, extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtils),scheduledTaskDTO);
                String template = ObjectMapping.asJsonStringPretty(pod);

                return jinJavaUtils.render(template);
            } catch (IOException e) {
                throw new TaskConfigurationException(e);
            }
        }
}

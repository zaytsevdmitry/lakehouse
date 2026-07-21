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

package org.lakehouse.taskexecutor.processor.spark;


import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.AbstractSparkDeployTaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.SparkDeployHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service(value = "sparkStandAloneClusterTaskProcessor")
public class SparkStandAloneClusterTaskProcessor extends AbstractSparkDeployTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String restConfUrl;
    private final String restSchedulerUrl;

    public SparkStandAloneClusterTaskProcessor(
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl) {
        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {

        SparkDeployHelper sparkDeployHelper = new SparkDeployHelper(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);

        deploy(
                scheduledTaskDTO.buildTaskFullName(),
                sparkDeployHelper.getMainClass(),
                sparkDeployHelper.getAppResource(),
                sparkDeployHelper.getMasterUrl(),
                sparkDeployHelper.getSparkConf(),
                sparkDeployHelper.getArgs(restConfUrl,restSchedulerUrl));
    }
}

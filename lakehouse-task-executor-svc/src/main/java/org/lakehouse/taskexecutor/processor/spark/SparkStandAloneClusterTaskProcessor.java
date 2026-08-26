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


import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.SparkRestClientApiImpl;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.AbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service(value = "sparkStandAloneClusterTaskProcessor")
public class SparkStandAloneClusterTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String restConfUrl;
    private final String restSchedulerUrl;
    private final String MAIN_CLASS_KEY = "deploy.mainClass";
    private final String APP_RESOURCE_KEY = "deploy.appResource";
    private final String CLUSTER_URL_KEY = "deploy.clusterUrl";
    private final RestClient.Builder restClientBuilder;
    private final Integer sparkJobStatusCheckIntervalMs;
    private final long maxWaitToRunningStateTimeoutMs;
    private final int DRIVER_STATE_NULL_LIMIT = 30;
    //todo other final status
    private final List<String> finalStatusNames = List.of("FINISHED", "KILLED", "FAILED", "ERROR");
    private final List<String> negativeStatusNames = List.of("KILLED", "FAILED" , "ERROR");


    public SparkStandAloneClusterTaskProcessor(
            RestClient.Builder restClientBuilder,
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl,
            @Value("${lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.sparkJobStatusCheckIntervalMs:3000}") Integer sparkJobStatusCheckIntervalMs,
            @Value("${lakehouse.task-executor.processor.sparkStandAloneClusterTaskProcessor.maxWaitToRunningStateTimeoutMs:3000}") long maxWaitToRunningStateTimeoutMs
    ) {

        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
        this.restClientBuilder = restClientBuilder;
        this.sparkJobStatusCheckIntervalMs = sparkJobStatusCheckIntervalMs;
        this.maxWaitToRunningStateTimeoutMs = maxWaitToRunningStateTimeoutMs;

    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {
        String targetDataSetKeyName = scheduledTaskDTO.getDataSetKeyName();

        Map<String, String> sparkProperties = SparkConfUtil.extractSparkConFromTaskConf(
                sourceConfDTO, scheduledTaskDTO);

        sourceConfDTO.getDataSources().forEach((s, dataSourceDTO) -> {
            if (dataSourceDTO.getDataSourceType().equals(Types.DataSourceType.database)){
                String key = String.format("spark.sql.catalog.%s.url", dataSourceDTO.getKeyName());
                if (!sparkProperties.containsKey(key)){
                    sparkProperties.put(
                            key,
                            dataSourceDTO.getDatabaseProtocol().buildConnectionStringTemplate(
                                    dataSourceDTO.getService().getHost(),
                                    Integer.parseInt(dataSourceDTO.getService().getPort()),
                                    dataSourceDTO.getService().getUrn()));
                }
            }
        });
        ScheduledTaskDTO unSparkedTaskConfig = SparkConfUtil.unSparkConf(scheduledTaskDTO);

        DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSourceDTOByDataSetKeyName(targetDataSetKeyName);
        String mainClass = Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(MAIN_CLASS_KEY),
                dataSourceDTO.getService().getProperties().get(MAIN_CLASS_KEY)
        );
        String appResource = Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(APP_RESOURCE_KEY),
                dataSourceDTO.getService().getProperties().get(APP_RESOURCE_KEY)
        );


        Map<String,String> argsMap = new HashMap<>(unSparkedTaskConfig.getTaskProcessorArgs());
        argsMap.put("scheduledTaskId",String.valueOf(scheduledTaskDTO.getId()));
        argsMap.put(ConfigRestClientConstants.restConfKey, restConfUrl);
        argsMap.put(SchedulerRestClientConstants.restSchedulerKey, restSchedulerUrl);

        List<String> appArgs = new ArrayList<>(argsMap
                .entrySet()
                .stream()
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .toList());

        deploy(
                mainClass,
                appResource,
                getMasterUrl(scheduledTaskDTO),
                sparkProperties,
                appArgs);
    }

    public boolean isStatusFinal(String statusName) {
        return statusName != null && finalStatusNames.contains(statusName);
    }

    public boolean isStatusNegative(String statusName) {
        return statusName != null && negativeStatusNames.contains(statusName);
    }

    public SparkRestClientApi buildSparkRestClientApi(String baseURI) {
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        RestClient restClient = restClientBuilder
                .clone()
                .uriBuilderFactory(defaultUriBuilderFactory)
                .build();
        return new SparkRestClientApiImpl(new RestClientHelper(restClient));
    }


    private void deploy(
            String mainClass,
            String appResource,
            String severUrl,
            Map<String, String> sparkProperties,
            List<String> sparkArgs) throws TaskFailedException, TaskConfigurationException {
        CreateRequest createRequest = new CreateRequest();

        createRequest.setMainClass(mainClass);
        createRequest.setAppResource(appResource);
        createRequest.setSparkProperties(sparkProperties);
        createRequest.setAppArgs(sparkArgs);
        CreateResponse createResponse = null;

        try {
            createResponse =buildSparkRestClientApi(severUrl).createSubmission(createRequest);
        } catch (RestClientResponseException e) {
            throw new TaskConfigurationException("Deploy failed. Response error", e);
        }

        logger.info(
                "Task  submitted as {}",
                createResponse.getSubmissionId());

        StatusResponse status = null;
        boolean isFinished = false;
        boolean isRunning = false;
        int nullDriverStateCount = 0;
        long startTime = System.currentTimeMillis();

        while (!isFinished) {
            sleep(sparkJobStatusCheckIntervalMs);
            status =
                    buildSparkRestClientApi(severUrl)
                            .getStatus(createResponse.getSubmissionId());
            logger.info("Spark job {} status {} {}", status.getSubmissionId(), status.getDriverState(),status.getMessage());

            if (!isRunning && "RUNNING".equals(status.getDriverState())) {
                isRunning = true;
                logger.info("Spark job {} reached RUNNING state", createResponse.getSubmissionId());
            }

            if (isStatusFinal(status.getDriverState())) {
                isFinished = true;
            } else if (!isRunning
                    && System.currentTimeMillis() - startTime > maxWaitToRunningStateTimeoutMs) {
                throw new TaskFailedException(String.format(
                        "Spark job did not reach RUNNING state within %d seconds. Last status: %s",
                        TimeUnit.MILLISECONDS.toSeconds(maxWaitToRunningStateTimeoutMs),
                        status.getDriverState()));
            }
            // Spark master reports success=false with no driverState once the
            // driver record is gone; without this check the poll loop never ends
            if (status.getDriverState() == null || "UNKNOWN".equals(status.getDriverState())) {
                if (isRunning) {
                    nullDriverStateCount++;
                    if ("UNKNOWN".equals(status.getDriverState())
                            || nullDriverStateCount >= DRIVER_STATE_NULL_LIMIT) {
                        throw new TaskFailedException(String.format(
                                "Spark job lost after RUNNING state (driverState=%s). Check cluster task information for submissionId=%s",
                                status.getDriverState(), status.getSubmissionId()));
                    }
                }
            } else {
                nullDriverStateCount = 0;
            }
        }
        if (isStatusNegative(status.getDriverState()))
            throw new TaskFailedException(String.format("Spark job state is %s. %s", status.getDriverState(), status.getMessage()));
    }
    public String getMasterUrl(
            ScheduledTaskDTO scheduledTaskDTO) throws TaskConfigurationException {

        String result =  scheduledTaskDTO.getTaskProcessorArgs().getOrDefault(CLUSTER_URL_KEY,"");

        if ("".equals(result))
            throw new TaskConfigurationException(String.format("Cluster url is empty %s", CLUSTER_URL_KEY));

        return result;
    }
}

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


import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.conf.ConfUtil;
import org.lakehouse.client.rest.kyuubi.BatchRequest;
import org.lakehouse.client.rest.kyuubi.BatchResponse;
import org.lakehouse.client.rest.kyuubi.KyuubiBatchClientApi;
import org.lakehouse.client.rest.kyuubi.KyuubiBatchClientFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.kyuubi.KyuubiDeployHelper;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.AbstractSparkDeployTaskProcessor;
import org.lakehouse.taskexecutor.processor.spark.standalonecluster.SparkDeployHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service(value = "kyuubiSparkBatchTaskProcessor")
public class KyuubiSparkBatchTaskProcessor extends AbstractSparkDeployTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String restConfUrl;
    private final String restSchedulerUrl;


    private final KyuubiBatchClientFactory kyuubiBatchClientFactory;

    public KyuubiSparkBatchTaskProcessor(
            KyuubiBatchClientFactory kyuubiBatchClientFactory,
            @Value("${lakehouse.client.rest.config.server.url}") String restConfUrl,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String restSchedulerUrl) {
        this.restConfUrl = restConfUrl;
        this.restSchedulerUrl = restSchedulerUrl;
        this.kyuubiBatchClientFactory = kyuubiBatchClientFactory;
    }

    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException, TaskConfigurationException {
        logger.info("Run task processor {} for task {}",this.getClass().getName(),scheduledTaskDTO.buildTaskFullName());
        Map<String,String> kyuubiConf = jinJavaUtils.renderMapValues(
                Coalesce.applyMergeNonNullValuesMap(
                        ConfUtil.extractConf(scheduledTaskDTO.getTaskProcessorArgs(), KyuubiDeployHelper.KYUUBI_PREFIX),
                        ConfUtil.extractConf(sourceConfDTO.getTargetDataSource().getService().getProperties(),KyuubiDeployHelper.KYUUBI_PREFIX)));

        String kyuubiUsername = kyuubiConf.getOrDefault(KyuubiDeployHelper.KYUUBI_USER_KEY,"");
        String kyuubiPassword = kyuubiConf.getOrDefault(KyuubiDeployHelper.KYUUBI_PASS_KEY,"");
        String kyuubiServerUrl = kyuubiConf.getOrDefault(KyuubiDeployHelper.KYUUBI_URL_KEY,"");

        if(kyuubiServerUrl.isBlank()){
            throw new TaskConfigurationException(String.format("Kyuubi Server Url is blank. Use property %s.%s",KyuubiDeployHelper.KYUUBI_PREFIX,KyuubiDeployHelper.KYUUBI_URL_KEY));
        }

        if (kyuubiServerUrl.endsWith("/v1/submissions")) {
            kyuubiServerUrl = kyuubiServerUrl.substring(0, kyuubiServerUrl.length() - "/v1/submissions".length());
        }

        SparkDeployHelper sparkDeployHelper = new SparkDeployHelper(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);
        BatchRequest batchRequest = new BatchRequest(
                "Spark",
                sparkDeployHelper.getAppResource(),
                sparkDeployHelper.getMainClass(),
                sparkDeployHelper.getArgs(restConfUrl,restSchedulerUrl),
                sparkDeployHelper.getSparkConf());

        // Explicitly map application name if provided by helper
        batchRequest.setName(scheduledTaskDTO.buildTaskFullName());

        KyuubiBatchClientApi kyuubiClient = kyuubiBatchClientFactory.createClient(kyuubiServerUrl, kyuubiUsername, kyuubiPassword);
        String batchId = null;

        try {
            logger.info("Submitting batch job to Kyuubi server: {}", kyuubiServerUrl);
            BatchResponse batchResponse = kyuubiClient.createBatch(batchRequest);
            batchId = batchResponse.getId();
            logger.info("Kyuubi batch job successfully submitted. Batch ID: {}", batchId);
            logger.debug(
                    "\n Request:\n{}\nResponse:\n{}",
                    ObjectMapping.asJsonStringPretty(batchRequest),
                    ObjectMapping.asJsonStringPretty(batchResponse));

            // --- FIXED: Added robust Polling loop to wait for job completion ---
            boolean isFinished = false;
            while (!isFinished) {
                // Poll intervals of 10 seconds to avoid overwhelming the Kyuubi REST endpoint
                Thread.sleep(10000);

                BatchResponse statusResponse = kyuubiClient.getBatchStatus(batchId);
                String currentState = statusResponse.getState();
                logger.info("Kyuubi Batch [{}] state: {}", batchId, currentState);
                logger.debug(
                        "\nResponse:\n{}",
                        ObjectMapping.asJsonStringPretty(batchResponse));
                // Inherited methods from AbstractSparkDeployTaskProcessor to check state boundaries
                if (isStatusFinal(currentState)) {
                    isFinished = true;
                    if (isStatusNegative(currentState)) {
                        logger.error("Kyuubi Batch [{}] failed or was canceled. Terminal state: {}", batchId, currentState);


                        String diagnostics = statusResponse.getAppDiagnostic();
                        String errorMessage = String.format(
                                "Kyuubi Batch job [%s] failed with status: %s. %s",
                                batchId,
                                currentState,
                                (diagnostics != null && !diagnostics.isBlank()) ? "Driver Diagnostics: " + diagnostics : "No diagnostic info available."
                        );

                        logger.error(errorMessage);

                        throw new TaskFailedException("Kyuubi Batch job failed with status: " + currentState);
                    }
                    logger.info("Kyuubi Batch [{}] completed successfully.", batchId);
                }
            }

        } catch (IOException | InterruptedException e) {
            // Handle execution interruptions gracefully by canceling the running batch on Kyuubi
            if (e instanceof InterruptedException && batchId != null) {
                logger.warn("Task execution interrupted. Attempting to kill Kyuubi Batch [{}]...", batchId);
                try {
                    kyuubiClient.cancelBatch(batchId);
                } catch (Exception ex) {
                    logger.error("Failed to cancel Kyuubi Batch [{}] on thread interruption", batchId, ex);
                }
                Thread.currentThread().interrupt();
            }
            throw new TaskFailedException(e);
        }
    }
}
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

package org.lakehouse.taskexecutor.processor.spark.k8snative;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.test.config.configuration.FileLoader;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EnableKubernetesMockClient(crud = true) // Fabric8 v7 JUnit5 extension for auto-injecting a CRUD-enabled mock client
public class K8sClientServiceTest {

    // Injected automatically by Fabric8 v7 Mock Server Extension
    private KubernetesClient client;

    static FileLoader fileLoader = new FileLoader();

    public static SourceConfDTO getSourceConfDTO() throws IOException {
        SourceConfDTO sourceConfDTO = new SourceConfDTO();
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("k8slakehousestorage");
        sourceConfDTO.setDataSources(
                Map.of(
                        "processingdb", fileLoader.loadDataSourceDTO("processingdb"),
                        dataSourceDTO.getKeyName(), dataSourceDTO
                )
        );

        DataSetDTO dataSetDTO = fileLoader.loadDataSetDTO("transaction_dds");
        dataSetDTO.setDataSourceKeyName(dataSourceDTO.getKeyName());
        sourceConfDTO.setDataSets(Map.of(
                "transaction_processing", fileLoader.loadDataSetDTO("transaction_processing"),
                "client_processing", fileLoader.loadDataSetDTO("client_processing"),
                dataSetDTO.getKeyName(), dataSetDTO
        ));
        sourceConfDTO.setDrivers(Map.of(
                "postgres", fileLoader.loadDriverDTO("postgres"),
                "spark_iceberg", fileLoader.loadDriverDTO("spark_iceberg")
        ));

        sourceConfDTO.setTargetDataSetKeyName(dataSetDTO.getKeyName());
        return sourceConfDTO;
    }

    public static ScheduledTaskDTO getScheduledTaskDTO() throws JsonProcessingException {
        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setName("quality");
        scheduledTaskDTO.setScheduleKeyName("Test_Schedule");
        scheduledTaskDTO.setScenarioActKeyName("Test_Act");
        scheduledTaskDTO.setTargetDateTime("2026-01-01 00:00:00z");
        scheduledTaskDTO.setTaskExecutionServiceGroupName("default");
        scheduledTaskDTO.setTaskProcessor("K8sSparkNativeTaskProcessor");
        scheduledTaskDTO.setTaskProcessorBody("sparkTaskProcessorDQBody");
        scheduledTaskDTO.setTryNum(1);
        scheduledTaskDTO.setId(100L);
        Map<String, String> taskProcessorArgs = Map.of(
                "spark.ui.enabled", "true",
                "spark.executor.memory", "1g",
                "k8s.spark-native.manifest.metadata.namespace", "lakehouse-management-ovrd",
                "lakehouse.client.rest.config.server.url", "http://lakehouse-management-config-service:8080",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers", "broker:9092",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic", "metric_value",
                "datasource.service.protocol", "https",
                "k8s.spark-native.command", "./bin/spark-submit",
                "k8s.spark-native.args", "--master k8s://{{ masterUrl }}\\\n {{sparkConf}} --class  org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ local:///opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.5.0-jar-with-dependencies.jar  {{appArgs}}"
        );

        scheduledTaskDTO.setTaskProcessorArgs(taskProcessorArgs);

        System.out.println(ObjectMapping.asJsonStringPretty(scheduledTaskDTO));
        return scheduledTaskDTO;
    }

    private String getJsonConf() throws IOException, TaskConfigurationException {
        PodUtilService podUtilService = new PodUtilService("","");
        K8sConfigService k8sConfigService = new K8sConfigService(podUtilService);
        return k8sConfigService.extractAppConfJson(getSourceConfDTO(), getScheduledTaskDTO());
    }

    @Test
    @DisplayName("Successful submit: pod is created from generated JSON, watcher triggers, and resources are cleaned up")
    void submit() throws Exception {
        // 1. Arrange
        String jsonConf = getJsonConf();

        // Create a Spy to mock the internal long-running watch behavior without affecting DSL parsing logic
        K8sClientService k8sClientServiceSpy = spy(new K8sClientService());

        // Stub the blocking watch execution to simulate immediate success
        doNothing().when(k8sClientServiceSpy).watchForSuccess(any(), any(), any(), anyLong(), anyLong());

        // Target namespace mapped from taskProcessorArgs inside getScheduledTaskDTO()
        String expectedNamespace = "lakehouse-management-ovrd";

        // 2. Act
        k8sClientServiceSpy.submit(
                client,
                jsonConf,
                5L,   // startupTimeoutMinutes
                10L,  // businessTimeoutMinutes
                true, // cleanUpIfFail
                false // logDeliveryIfFail
        );

        // 3. Assert
        // Verify core infrastructure interactions within the submit scope
        verify(k8sClientServiceSpy, times(1)).createResource(eq(client), any(Pod.class));

        verify(k8sClientServiceSpy, times(1))
                .watchForSuccess(eq(client), eq(expectedNamespace), anyString(), eq(5L), eq(10L));

        // Resource cleanup must execute exactly twice: pre-flight cleanup check and finally-block teardown
        verify(k8sClientServiceSpy, times(2))
                .cleanUpResource(eq(client), eq(expectedNamespace), anyString());

        // Ensure error pathways are untouched during the happy path run
        verify(k8sClientServiceSpy, never()).logDelivery(any(), any(), any());
    }

    @Test
    @DisplayName("Submit failure: if watcher throws an exception, TaskFailedException is propagated and resources are cleaned up")
    void submitShouldHandleFailureAndCleanup() throws Exception {
        // 1. Arrange
        String jsonConf = getJsonConf();
        K8sClientService k8sClientServiceSpy = spy(new K8sClientService());
        String expectedNamespace = "lakehouse-management-ovrd";

        // Force the watch phase to simulate a standard execution timeout failure
        doThrow(new TaskFailedException("Spark job execution timed out."))
                .when(k8sClientServiceSpy).watchForSuccess(any(), any(), any(), anyLong(), anyLong());

        // 2. Act & Assert
        TaskFailedException exception = assertThrows(TaskFailedException.class, () -> {
            k8sClientServiceSpy.submit(client, jsonConf, 2L, 5L, true, false);
        });

        assertEquals("Spark job execution timed out.", exception.getMessage());

        // Verify that cleanup sequence triggers even if the operational execution scope crashes
        verify(k8sClientServiceSpy, times(2))
                .cleanUpResource(eq(client), eq(expectedNamespace), anyString());
    }
}

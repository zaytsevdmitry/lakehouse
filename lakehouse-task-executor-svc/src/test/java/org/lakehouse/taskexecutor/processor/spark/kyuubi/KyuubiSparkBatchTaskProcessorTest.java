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
package org.lakehouse.taskexecutor.processor.spark.kyuubi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.kyuubi.BatchRequest;
import org.lakehouse.client.rest.kyuubi.BatchResponse;
import org.lakehouse.client.rest.kyuubi.KyuubiBatchClientApi;
import org.lakehouse.client.rest.kyuubi.KyuubiBatchClientFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.processor.spark.KyuubiSparkBatchTaskProcessor;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KyuubiSparkBatchTaskProcessorTest {

    @Mock
    private KyuubiBatchClientFactory clientFactory;

    @Mock
    private KyuubiBatchClientApi clientApi;

    @Mock
    private JinJavaUtils jinJavaUtils;

    private KyuubiSparkBatchTaskProcessor processor;

    private final ConfigRestClientApi configRestClientApi = new ConfigRestClientApiTest();
    private SourceConfDTO sourceConfDTO ;
    private ScheduledTaskDTO scheduledTaskDTO;

    private static final String REST_CONF_URL = "http://conf-server:8081";
    private static final String REST_SCHEDULER_URL = "http://scheduler-server:8082";

    KyuubiSparkBatchTaskProcessorTest() throws IOException {
    }

    @BeforeEach
    void setUp() {
        processor = new KyuubiSparkBatchTaskProcessor(clientFactory, REST_CONF_URL, REST_SCHEDULER_URL);
        
        // Initialize DTO structures to prevent NullPointerExceptions during helper extraction
        sourceConfDTO = configRestClientApi.getSourceConfDTO("transaction_dds");
       /* DataSourceDTO targetDataSource = new DataSourceDTO();
        targetDataSource.setKeyName("testdatasource");
        ServiceDTO service = new ServiceDTO();
        Map<String, String> serviceProperties = new HashMap<>();
       */
        ServiceDTO service = sourceConfDTO.getTargetDataSource().getService();
        Map<String, String> serviceProperties = new HashMap<>(service.getProperties());
        // Add Kyuubi URL and basic credentials via Service properties
        serviceProperties.put(KyuubiDeployHelper.KYUUBI_PREFIX + KyuubiDeployHelper.KYUUBI_URL_KEY, "http://kyuubi-server:10099/v1/submissions");
        serviceProperties.put(KyuubiDeployHelper.KYUUBI_PREFIX + KyuubiDeployHelper.KYUUBI_USER_KEY, "test-user");
        serviceProperties.put(KyuubiDeployHelper.KYUUBI_PREFIX + KyuubiDeployHelper.KYUUBI_PASS_KEY, "test-pass");
        
        service.setProperties(serviceProperties);
        sourceConfDTO.getTargetDataSource().setService(service);


        scheduledTaskDTO = new ScheduledTaskDTO();
        TaskDTO taskDTO = new TaskDTO();
        scheduledTaskDTO.setName("Test_Kyuubi_ETL_Job");
        scheduledTaskDTO.setTaskProcessorArgs(new HashMap<>()); // empty processor args
        scheduledTaskDTO.setTargetDateTime("2026-07-19 01:00:00z");
        scheduledTaskDTO.setScheduleKeyName("TestSchedule");
        scheduledTaskDTO.setScenarioActKeyName("TestScenarioAct");
        scheduledTaskDTO.setId(1L);
        scheduledTaskDTO.setTryNum(10);
    }

    @Test
    void testRunTask_Success_WithPollingLoop() throws Exception {
        // Given
        // Mock JinJavaUtils behavior to return input map values as-is
        when(jinJavaUtils.renderMapValues(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Setup factory mock to yield our API client mock
        when(clientFactory.createClient("http://kyuubi-server:10099", "test-user", "test-pass"))
                .thenReturn(clientApi);

        // Setup mock lifecycle transitions for Kyuubi Batch Job
        BatchResponse createResponse = new BatchResponse();
        createResponse.setId("batch-uuid-999");
        when(clientApi.createBatch(any(BatchRequest.class))).thenReturn(createResponse);

        BatchResponse runningResponse = new BatchResponse();
        runningResponse.setState("RUNNING");

        BatchResponse finishedResponse = new BatchResponse();
        finishedResponse.setState("FINISHED");

        // First poll returns RUNNING, second returns FINISHED
        when(clientApi.getBatchStatus("batch-uuid-999"))
                .thenReturn(runningResponse)
                .thenReturn(finishedResponse);

        // When
        assertDoesNotThrow(() -> processor.runTask(sourceConfDTO, scheduledTaskDTO, jinJavaUtils));

        // Then
        // Verify that the path was truncated correctly (omitted /v1/submissions)
        verify(clientFactory, times(1)).createClient("http://kyuubi-server:10099", "test-user", "test-pass");
        
        // Capture and assert properties inside BatchRequest mapping
        ArgumentCaptor<BatchRequest> requestCaptor = ArgumentCaptor.forClass(BatchRequest.class);
        verify(clientApi, times(1)).createBatch(requestCaptor.capture());
        
        BatchRequest capturedRequest = requestCaptor.getValue();
        assertEquals("Spark", capturedRequest.getBatchType());
        assertEquals("1-10-TestSchedule-TestScenarioAct-Test_Kyuubi_ETL_Job-20260719 010000z", capturedRequest.getName());

        // Verify polling occurred exactly twice before termination
        verify(clientApi, times(2)).getBatchStatus("batch-uuid-999");
    }

    @Test
    void testRunTask_ThrowsTaskConfigurationException_WhenUrlIsBlank() {
        // Given
        // Clear out the Kyuubi URL property to simulate misconfiguration
        sourceConfDTO.getTargetDataSource().getService().getProperties().clear();
        when(jinJavaUtils.renderMapValues(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        TaskConfigurationException exception = assertThrows(TaskConfigurationException.class, () -> 
                processor.runTask(sourceConfDTO, scheduledTaskDTO, jinJavaUtils)
        );
        assertTrue(exception.getMessage().contains("Kyuubi Server Url is blank"));
    }

    @Test
    void testRunTask_ThrowsTaskFailedException_WhenJobFails() throws Exception {
        // Given
        when(jinJavaUtils.renderMapValues(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientFactory.createClient(any(), any(), any())).thenReturn(clientApi);

        BatchResponse createResponse = new BatchResponse();
        createResponse.setId("batch-uuid-failure");
        when(clientApi.createBatch(any())).thenReturn(createResponse);

        BatchResponse errorResponse = new BatchResponse();
        errorResponse.setState( "ERROR"); // Terminal negative state handled by AbstractSparkDeployTaskProcessor
        
        when(clientApi.getBatchStatus("batch-uuid-failure")).thenReturn(errorResponse);

        // When & Then
        TaskFailedException exception = assertThrows(TaskFailedException.class, () -> 
                processor.runTask(sourceConfDTO, scheduledTaskDTO, jinJavaUtils)
        );
        assertTrue(exception.getMessage().contains("Kyuubi Batch job failed with status: ERROR"));
    }
    @Test
    void testRunTask_HandlesThreadInterruption_AndCancelsKyuubiBatch() throws Exception {
        // Given
        when(jinJavaUtils.renderMapValues(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientFactory.createClient(any(), any(), any())).thenReturn(clientApi);

        BatchResponse createResponse = new BatchResponse();
        createResponse.setId("batch-uuid-interrupted");
        when(clientApi.createBatch(any())).thenReturn(createResponse);

        // Симулируем, что первая же проверка статуса приводит к прерыванию потока.
        // Вместо реальной паузы в Thread.sleep() мы заставляем метод getBatchStatus
        // выбросить InterruptedException, чтобы съэмулировать асинхронное прерывание извне.
        when(clientApi.getBatchStatus("batch-uuid-interrupted"))
                .thenThrow(new InterruptedException("Orchestrator interrupted the thread"));

        // When & Then
        TaskFailedException exception = assertThrows(TaskFailedException.class, () ->
                processor.runTask(sourceConfDTO, scheduledTaskDTO, jinJavaUtils)
        );

        // Проверяем, что исходная причина была обернута в TaskFailedException
        assertNotNull(exception.getCause());
        assertTrue(exception.getCause() instanceof InterruptedException);

        // ГЛАВНАЯ ПРОВЕРКА: Убеждаемся, что процессор гарантированно отправил
        // команду отмены (cancelBatch) на сервер Kyuubi для удаления Spark-приложения
        verify(clientApi, times(1)).cancelBatch("batch-uuid-interrupted");
    }

}

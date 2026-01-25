package org.lakehouse.taskexecutor.test;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.taskexecutor.processor.state.DependencyCheckStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.LockedStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.SuccessStateTaskProcessor;
import org.lakehouse.taskexecutor.test.stub.SparkRestClientApiTest;
import org.lakehouse.taskexecutor.test.stub.StateRestClientApiTest;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@ContextConfiguration(classes = {

        SparkRestClientApiTest.class
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StateAdapterTest {

    FileLoader fileLoader = new FileLoader();

    @Autowired
    SparkRestClientApi sparkRestClientApi;

    @Test
    @Order(1)
    public void testSendStateSUCCESS() throws IOException, TaskFailedException {
        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");

        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSetKeyName(testDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);


        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest();
        SuccessStateTaskProcessor processor = new SuccessStateTaskProcessor(tpc, stateRestClientApi);
        processor.runTask();
    }


    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorFAILED() throws Exception {

        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");

        //datasets target and dependency
        DataSetDTO testTargetDataSet = fileLoader.loadDataSetDTO("transaction_dds");
        DataSetDTO testDependencyDataSet = fileLoader.loadDataSetDTO("client_processing");

        //config
        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        tpc.setTargetDataSetKeyName(testTargetDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);
        tpc.setDataSets(Map.of(
                testTargetDataSet.getKeyName(),testTargetDataSet,
                testDependencyDataSet.getKeyName(),testDependencyDataSet));

        //tpc.setSources(Map.of(testDependencyDataSet.getKeyName(), testDependencyDataSet));

        //History locked record
        DataSetStateDTO dataSetStateDTOLocked = new DataSetStateDTO();
        dataSetStateDTOLocked.setDataSetKeyName(testDependencyDataSet.getKeyName());
        dataSetStateDTOLocked.setStatus(Status.DataSet.LOCKED);
        dataSetStateDTOLocked.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(tpc.getIntervalStartDateTime()));
        dataSetStateDTOLocked.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(tpc.getIntervalEndDateTime()));
        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest(List.of(dataSetStateDTOLocked));

        // Execute check
        DependencyCheckStateTaskProcessor processor = new DependencyCheckStateTaskProcessor(tpc, stateRestClientApi);
        try {
            processor.runTask();
            throw new Exception("Expect failure, but ....");
        } catch (TaskFailedException e) {
            // Failed because it was Expected
        }
    }

    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorSUCCESS() throws IOException, TaskFailedException {

        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");


        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSetKeyName(testDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);

        tpc.setDataSets(Map.of(testDataSet.getKeyName(),testDataSet));

        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest();
        DependencyCheckStateTaskProcessor processor = new DependencyCheckStateTaskProcessor(tpc, stateRestClientApi);
        processor.runTask();
    }

    @Test
    @Order(3)
    public void testBeginTaskProcessorRUNNING() throws IOException, TaskFailedException {

        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");


        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSetKeyName(testDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);

        tpc.setDataSets(Map.of(testDataSet.getKeyName(),testDataSet));

        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest();
        LockedStateTaskProcessor processor = new LockedStateTaskProcessor(tpc, stateRestClientApi);
        processor.runTask();
    }
}

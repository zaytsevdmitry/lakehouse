package org.lakehouse.taskexecutor.test;

import com.hubspot.jinjava.Jinjava;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.executionmodule.DependencyCheckTaskProcessor;
import org.lakehouse.taskexecutor.executionmodule.StateSuccessTaskProcessor;
import org.lakehouse.taskexecutor.test.state.DependencyCheckStateRestClientApiTest;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootTest( properties = {"spring.main.allow-bean-definition-overriding=true"})
/*
@ComponentScan(basePackages = {
        "org.lakehouse.taskexecutor"
}
)*/

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StateAdapterTest {

    FileLoader fileLoader = new FileLoader();

    @Test
    @Order(1)
    public void testSendStateSUCCESS() throws IOException {
        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end   = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");

        TaskProcessorConfig tpc = new TaskProcessorConfig();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSet(testDataSet);
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);


        DependencyCheckStateRestClientApiTest stateRestClientApi = new DependencyCheckStateRestClientApiTest();
        StateSuccessTaskProcessor processor = new StateSuccessTaskProcessor(tpc, stateRestClientApi, new Jinjava());
        assert (processor.runTask().equals(Status.Task.SUCCESS));


    }


    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorFAILED() throws IOException {

        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end   = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");

        //datasets target and dependency
        DataSetDTO testTargetDataSet = fileLoader.loadDataSetDTO("transaction_dds");
        DataSetDTO testDependencyDataSet = fileLoader.loadDataSetDTO("client_processing");

        //config
        TaskProcessorConfig tpc = new TaskProcessorConfig();
        tpc.setTargetDataSet(testTargetDataSet);
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);
        tpc.setDataSetDTOSet(Set.of(testTargetDataSet,testDependencyDataSet));
        tpc.setSources(Map.of(testDependencyDataSet.getKeyName(),testDependencyDataSet));

        //History wrong record
        DataSetStateDTO dataSetStateDTOWrong = new DataSetStateDTO();
        dataSetStateDTOWrong.setDataSetKeyName(testDependencyDataSet.getKeyName());
        dataSetStateDTOWrong.setStatus(Status.DataSet.FAILED.label);
        dataSetStateDTOWrong.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(tpc.getIntervalStartDateTime()));
        dataSetStateDTOWrong.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(tpc.getIntervalEndDateTime()));
        DependencyCheckStateRestClientApiTest stateRestClientApi = new DependencyCheckStateRestClientApiTest(List.of(dataSetStateDTOWrong));

        // Execute check
        DependencyCheckTaskProcessor processor = new DependencyCheckTaskProcessor(tpc,stateRestClientApi,new Jinjava());
        assert (processor.runTask().equals(Status.Task.FAILED));
    }
    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorSUCCESS() throws IOException {

        OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end   = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");




        TaskProcessorConfig tpc = new TaskProcessorConfig();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSet(testDataSet);
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);

        tpc.setDataSetDTOSet(Set.of(testDataSet));

        DependencyCheckStateRestClientApiTest stateRestClientApi = new DependencyCheckStateRestClientApiTest();
        DependencyCheckTaskProcessor processor = new DependencyCheckTaskProcessor(tpc,stateRestClientApi,new Jinjava());
        assert (processor.runTask().equals(Status.Task.SUCCESS));
    }
 }

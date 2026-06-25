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

package org.lakehouse.taskexecutor.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.lakehouse.taskexecutor.processor.state.DependencyCheckStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.LockedStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.state.SuccessStateTaskProcessor;
import org.lakehouse.taskexecutor.test.stub.StateRestClientApiTest;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
/*@ContextConfiguration(classes = {

        SparkRestClientApiTest.class, StateRestClientApiTest.class, ConfigRestClientApiTest.class
})*/
@Import({SuccessStateTaskProcessor.class, DependencyCheckStateTaskProcessor.class, LockedStateTaskProcessor.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StateAdapterTest {
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        @Primary
        StateRestClientApi getStateRestClientApi(){
            return new StateRestClientApiTest();
        }

    }
    @Autowired
    ConfigRestClientApi configRestClientApi;
    @Autowired
    ConfigurableApplicationContext applicationContext;

    FileLoader fileLoader = new FileLoader();

    @Autowired
    StateRestClientApi stateRestClientApi;



    private void runTaskProcessor(
            ScheduledTaskDTO scheduledTaskDTO)
            throws TaskConfigurationException, TaskFailedException, JsonProcessingException {

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        ((TaskProcessor) applicationContext.getBean(scheduledTaskDTO.getTaskProcessor())).runTask(sourceConfDTO,scheduledTaskDTO,jinJavaUtils);
    }

    private ScheduledTaskDTO getTargetScheduledTaskDTO(String dataSetName, Class aClass){
        String start = "2025-01-01T00:00:00z";
        String end = "2025-01-02T00:00:00z";
        char[] c = aClass.getSimpleName().toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        String prName= new String(c);


        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setId(1L);
        scheduledTaskDTO.setDataSetKeyName(dataSetName);
        scheduledTaskDTO.setIntervalStartDateTime(start);
        scheduledTaskDTO.setIntervalEndDateTime(end);
        scheduledTaskDTO.setTaskProcessor(prName);

        return scheduledTaskDTO;
    }
    @Test
    @Order(1)
    public void testSendStateSUCCESS() throws IOException, TaskFailedException, TaskConfigurationException {

        ScheduledTaskDTO scheduledTaskDTO = getTargetScheduledTaskDTO("client_processing", SuccessStateTaskProcessor.class);
                new ScheduledTaskDTO();
        runTaskProcessor(scheduledTaskDTO);
    }


    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorFAILED() throws Exception {
        //datasets target and dependency
        String testTargetDataSet = "transaction_dds";
        String dependencyDataSet = "client_processing";

        ScheduledTaskDTO scheduledTaskDTO =getTargetScheduledTaskDTO(testTargetDataSet,DependencyCheckStateTaskProcessor.class);



        //config
    //    SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(testTargetDataSet.getKeyName());
        /*
        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        tpc.setTargetDataSetKeyName(testTargetDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);
        tpc.setDataSets(Map.of(
                testTargetDataSet.getKeyName(),testTargetDataSet,
                testDependencyDataSet.getKeyName(),testDependencyDataSet));
*/
        //tpc.setSources(Map.of(testDependencyDataSet.getKeyName(), testDependencyDataSet));

        //History locked record
        DataSetStateDTO dataSetStateDTOLocked = new DataSetStateDTO();
        dataSetStateDTOLocked.setDataSetKeyName(dependencyDataSet);
        dataSetStateDTOLocked.setStatus(Status.DataSet.LOCKED);
        dataSetStateDTOLocked.setIntervalStartDateTime(scheduledTaskDTO.getIntervalStartDateTime());
        dataSetStateDTOLocked.setIntervalEndDateTime(scheduledTaskDTO.getIntervalEndDateTime());


        stateRestClientApi.setDataSetStateDTO(dataSetStateDTOLocked);


        // Execute check
       // DependencyCheckStateTaskProcessor processor = new DependencyCheckStateTaskProcessor( stateRestClientApi);
        try {
            runTaskProcessor(scheduledTaskDTO);
            //processor.runTask();
            throw new Exception("Expect failure, but ....");
        } catch (TaskFailedException e) {
            // Failed because it was Expected
        }
    }

    @Test
    @Order(2)
    public void testDependencyCheckTaskProcessorSUCCESS() throws IOException, TaskFailedException, TaskConfigurationException {

        //datasets target and dependency
        String testTargetDataSet = "client_processing";

        ScheduledTaskDTO scheduledTaskDTO = getTargetScheduledTaskDTO(testTargetDataSet, DependencyCheckStateTaskProcessor.class);

        runTaskProcessor(scheduledTaskDTO);

        /*TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSetKeyName(testDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);

        tpc.setDataSets(Map.of(testDataSet.getKeyName(),testDataSet));

        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest();
        DependencyCheckStateTaskProcessor processor = new DependencyCheckStateTaskProcessor(tpc, stateRestClientApi);
        processor.runTask();*/
    }

    @Test
    @Order(3)
    public void testBeginTaskProcessorRUNNING() throws IOException, TaskFailedException, TaskConfigurationException {
        ScheduledTaskDTO scheduledTaskDTO = getTargetScheduledTaskDTO("client_processing",LockedStateTaskProcessor.class);
        runTaskProcessor(scheduledTaskDTO);
        /* OffsetDateTime start = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z");
        OffsetDateTime end = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T00:00:00z");


        TaskProcessorConfigDTO tpc = new TaskProcessorConfigDTO();
        DataSetDTO testDataSet = fileLoader.loadDataSetDTO("client_processing");
        tpc.setTargetDataSetKeyName(testDataSet.getKeyName());
        tpc.setIntervalStartDateTime(start);
        tpc.setIntervalEndDateTime(end);

        tpc.setDataSets(Map.of(testDataSet.getKeyName(),testDataSet));

        StateRestClientApiTest stateRestClientApi = new StateRestClientApiTest();
        LockedStateTaskProcessor processor = new LockedStateTaskProcessor(tpc, stateRestClientApi);
        processor.runTask();*/
    }
}

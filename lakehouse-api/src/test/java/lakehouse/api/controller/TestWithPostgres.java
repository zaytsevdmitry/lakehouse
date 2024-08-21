package lakehouse.api.controller;

import lakehouse.api.configurationtest.FileLoader;
import lakehouse.api.configurationtest.RestManipulator;
import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.configs.*;
import lakehouse.api.service.tasks.ScheduleInstanceLastService;
import lakehouse.api.service.tasks.ScheduleInstanceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestWithPostgres {
    @Autowired
    ScheduleInstanceService scheduleInstanceService;
    @Autowired
    ScheduleInstanceLastService scheduleInstanceLastService;
    @Autowired
    FileLoader fileLoader;
    @Autowired
    RestManipulator restManipulator;


    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    ).withDatabaseName("test")
            .withUsername("name")
            .withPassword("password");

    @LocalServerPort
    private Integer port;



    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }


    private ProjectDTO putProjectDTO() throws Exception {
        ProjectDTO dto = fileLoader.loadProjectDTO();

        return fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(dto.getName(), fileLoader.asJsonString(dto), Endpoint.PROJECTS, Endpoint.PROJECTS_NAME),
                ProjectDTO.class);
    }

    @Test
    void shouldTestProjectDTO() throws Exception {
        ProjectDTO dto = fileLoader.loadProjectDTO();
        ProjectDTO resultDTO = putProjectDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.PROJECTS_NAME);
        assert (resultDTO.equals(dto));
    }

    private TaskExecutionServiceGroupDTO putTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();

        return fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(
                        dto.getName(),
                        fileLoader.asJsonString(dto),
                        Endpoint.TASK_EXECUTION_SERVICE_GROUPS,
                        Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME),
                TaskExecutionServiceGroupDTO.class);
    }

    @Test
    void shouldTestTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();
        TaskExecutionServiceGroupDTO resultDTO = putTaskExecutionServiceGroupDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        assert (resultDTO.equals(dto));
    }

    private ScenarioActTemplateDTO putScenarioDTO() throws Exception {
        ScenarioActTemplateDTO dto = fileLoader.loadScenarioDTO();
        return fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(dto.getName(), fileLoader.asJsonString(dto), Endpoint.SCENARIOS, Endpoint.SCENARIOS_NAME),
                ScenarioActTemplateDTO.class);
    }

    @Test
    void shouldTestScenarioDTO() throws Exception {
        TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO dto = fileLoader.loadScenarioDTO();
        ScenarioActTemplateDTO resultDTO = putScenarioDTO();

        assert (resultDTO.equals(dto));

        restManipulator.deleteDTO(dto.getName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(taskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
    }

    private DataStoreDTO putDataStoreDTO(String name) throws Exception {
        DataStoreDTO dto = fileLoader.loadDataStoreDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.DATA_STORES, Endpoint.DATA_STORES_NAME),
                DataStoreDTO.class);
    }

    @Test
    void shouldTestDataStoreDTO() throws Exception {
        DataStoreDTO dto = fileLoader.loadDataStoreDTO("processingdb");
        DataStoreDTO resultDTO = putDataStoreDTO("processingdb");

        restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_STORES_NAME);
        assert (resultDTO.equals(dto));
    }

    private DataSetDTO putDataSetDTO(String name) throws Exception {
        DataSetDTO dto = fileLoader.loadDataSetDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME),
                DataSetDTO.class);
    }

    @Test
    void shouldTestDataSetDTO() throws Exception {
        DataStoreDTO dataStoreDTO = putDataStoreDTO("processingdb");
        ProjectDTO projectDTO = putProjectDTO();
        DataSetDTO dto = putDataSetDTO("client_processing");

        DataSetDTO resultDTO = fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(
                        dto.getName(),
                        fileLoader.asJsonString(dto),
                        Endpoint.DATA_SETS,
                        Endpoint.DATA_SETS_NAME),
                DataSetDTO.class);
        restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
        restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);
        assert (resultDTO.equals(dto));
    }

    private ScheduleDTO putScheduleDTO(String name) throws Exception {
        ScheduleDTO dto = fileLoader.loadScheduleDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.SCHEDULES, Endpoint.SCHEDULES_NAME),
                ScheduleDTO.class);
    }

    @Test
    void shouldTestAllDTO() throws Exception {

        ProjectDTO projectDTO = putProjectDTO();
        //datastores
        DataStoreDTO someelsedbDataStoreDTO = putDataStoreDTO("processingdb");
        DataStoreDTO mydbDataStoreDTO = putDataStoreDTO("lakehousestorage");
        //datasets
        DataSetDTO clientProcessingDTO = putDataSetDTO("client_processing");
        DataSetDTO transactionProcessingDTO = putDataSetDTO("transaction_processing");
        DataSetDTO resultTransactionddsDTO = putDataSetDTO("transaction_dds");
        DataSetDTO sourceTransactionddsDTO = fileLoader.loadDataSetDTO(resultTransactionddsDTO.getName());
        DataSetDTO resultTransactionddsDTOV2 = putDataSetDTO("transaction_dds_v2");
        DataSetDTO sourceTransactionddsDTOV2 = fileLoader.loadDataSetDTO("transaction_dds_v2");
        DataSetDTO resultAggdaily = putDataSetDTO("aggregation_pay_per_client_daily_mart");
        DataSetDTO sourceAggdaily = fileLoader.loadDataSetDTO("aggregation_pay_per_client_daily_mart");
        DataSetDTO resultAggTotal = putDataSetDTO("aggregation_pay_per_client_total_mart");
        DataSetDTO sourceAggTotal = fileLoader.loadDataSetDTO("aggregation_pay_per_client_total_mart");
        assert (resultAggdaily.equals(sourceAggdaily));
        assert (resultAggTotal.equals(sourceAggTotal));
        assert (resultTransactionddsDTO.equals(sourceTransactionddsDTO));
        assert (resultTransactionddsDTOV2.equals(sourceTransactionddsDTOV2));

        TaskExecutionServiceGroupDTO defaultTaskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO scenarioActTemplateDTO = putScenarioDTO();
        //schedules
        ScheduleDTO initialScheduleDTO = fileLoader.loadScheduleDTO("initial");
        ScheduleDTO regularScheduleDTO = fileLoader.loadScheduleDTO("regular");
        ScheduleDTO resultInitialScheduleDTO = putScheduleDTO("initial");
        ScheduleDTO resultRegularScheduleDTO = putScheduleDTO("regular");
        assert (resultInitialScheduleDTO.equals(initialScheduleDTO));
        assert (resultRegularScheduleDTO.equals(regularScheduleDTO));

        //tasks
        try {


           scheduleInstanceLastService.findAndRegisterNewSchedules();
           scheduleInstanceService.buildNewSchedules();
        }catch (Exception e){
            e.printStackTrace();
        }
        // delete
        restManipulator.deleteDTO(resultInitialScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);
        restManipulator.deleteDTO(resultRegularScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);

        restManipulator.deleteDTO(resultAggdaily.getName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);

        restManipulator.deleteDTO(scenarioActTemplateDTO.getName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
        restManipulator.deleteDTO(someelsedbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
        restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);


    }



}

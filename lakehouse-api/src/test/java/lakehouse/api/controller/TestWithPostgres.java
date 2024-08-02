package lakehouse.api.controller;

import lakehouse.api.configurationtest.FileLoader;
import lakehouse.api.configurationtest.RestManipulator;
import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.*;
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
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    ).withDatabaseName("test")
            .withUsername("name")
            .withPassword("password");
    @Autowired
    FileLoader fileLoader;
    @Autowired
    RestManipulator restManipulator;
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
                restManipulator.writeAndReadDTOTest(dto.getName(), fileLoader.asJsonString(dto), Endpoint.PROJECT, Endpoint.PROJECT_NAME),
                ProjectDTO.class);
    }

    @Test
    void shouldTestProjectDTO() throws Exception {
        ProjectDTO dto = fileLoader.loadProjectDTO();
        ProjectDTO resultDTO = putProjectDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.PROJECT_NAME);
        assert (resultDTO.equals(dto));
    }

    private TaskExecutionServiceGroupDTO putTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();

        return fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(
                        dto.getName(),
                        fileLoader.asJsonString(dto),
                        Endpoint.TASK_EXECUTION_SERVICE_GROUP,
                        Endpoint.TASK_EXECUTION_SERVICE_GROUP_NAME),
                TaskExecutionServiceGroupDTO.class);
    }

    @Test
    void shouldTestTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();
        TaskExecutionServiceGroupDTO resultDTO = putTaskExecutionServiceGroupDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUP_NAME);
        assert (resultDTO.equals(dto));
    }

    private ScenarioDTO putScenarioDTO() throws Exception {
        ScenarioDTO dto = fileLoader.loadScenarioDTO();
        return fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(dto.getName(), fileLoader.asJsonString(dto), Endpoint.SCENARIO, Endpoint.SCENARIO_NAME),
                ScenarioDTO.class);
    }

    @Test
    void shouldTestScenarioDTO() throws Exception {
        TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioDTO dto = fileLoader.loadScenarioDTO();
        ScenarioDTO resultDTO = putScenarioDTO();

        assert (resultDTO.equals(dto));

        restManipulator.deleteDTO(dto.getName(), Endpoint.SCENARIO_NAME);
        restManipulator.deleteDTO(taskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUP_NAME);
    }

    private DataStoreDTO putDataStoreDTO(String name) throws Exception {
        DataStoreDTO dto = fileLoader.loadDataStoreDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.DATA_STORE, Endpoint.DATA_STORE_NAME),
                DataStoreDTO.class);
    }

    @Test
    void shouldTestDataStoreDTO() throws Exception {
        DataStoreDTO dto = fileLoader.loadDataStoreDTO("someelsedb");
        DataStoreDTO resultDTO = putDataStoreDTO("someelsedb");

        restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_STORE_NAME);
        assert (resultDTO.equals(dto));
    }

    private DataSetDTO putDataSetDTO(String name) throws Exception {
        DataSetDTO dto = fileLoader.loadDataSetDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.DATA_SET, Endpoint.DATA_SET_NAME),
                DataSetDTO.class);
    }

    @Test
    void shouldTestDataSetDTO() throws Exception {
        DataStoreDTO dataStoreDTO = putDataStoreDTO("someelsedb");
        ProjectDTO projectDTO = putProjectDTO();
        DataSetDTO dto = putDataSetDTO("anotherTable");

        DataSetDTO resultDTO = fileLoader.stringToObject(
                restManipulator.writeAndReadDTOTest(
                        dto.getName(),
                        fileLoader.asJsonString(dto),
                        Endpoint.DATA_SET,
                        Endpoint.DATA_SET_NAME),
                DataSetDTO.class);
        restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_SET_NAME);
        restManipulator.deleteDTO(dataStoreDTO.getName(), Endpoint.DATA_STORE_NAME);
        restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECT_NAME);
        assert (resultDTO.equals(dto));
    }

    private ScheduleDTO putScheduleDTO(String name) throws Exception {
        ScheduleDTO dto = fileLoader.loadScheduleDTO(name);
        return fileLoader.stringToObject(
                restManipulator
                        .writeAndReadDTOTest(
                                dto.getName(),
                                fileLoader.asJsonString(dto), Endpoint.SCHEDULE, Endpoint.SCHEDULE_NAME),
                ScheduleDTO.class);
    }

    @Test
    void shouldTestAllDTO() throws Exception {
        ProjectDTO projectDTO = putProjectDTO();
        DataStoreDTO someelsedbDataStoreDTO = putDataStoreDTO("someelsedb");
        DataStoreDTO mydbDataStoreDTO = putDataStoreDTO("mydb");
        DataSetDTO otherTableSetDTO = putDataSetDTO("otherTable");
        DataSetDTO anotherTableSetDTO = putDataSetDTO("anotherTable");
        DataSetDTO resultMytabledataSetSetDTO = putDataSetDTO("mytabledataSet");
        DataSetDTO sourcetMytabledataSetSetDTO = fileLoader.loadDataSetDTO(resultMytabledataSetSetDTO.getName());
        DataSetDTO resultMytabledataSetSetDTOV2 = putDataSetDTO("mytabledataSetV2");
        DataSetDTO sourcetMytabledataSetSetDTOV2 = fileLoader.loadDataSetDTO("mytabledataSetV2");
        TaskExecutionServiceGroupDTO defaultTaskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioDTO scenarioDTO = putScenarioDTO();

        ScheduleDTO initialScheduleDTO = fileLoader.loadScheduleDTO("initial");
        ScheduleDTO regularScheduleDTO = fileLoader.loadScheduleDTO("regular");
        ScheduleDTO resultInitialScheduleDTO = putScheduleDTO("initial");
        ScheduleDTO resultRegularScheduleDTO = putScheduleDTO("regular");

        restManipulator.deleteDTO(resultInitialScheduleDTO.getName(), Endpoint.SCHEDULE_NAME);
        restManipulator.deleteDTO(resultRegularScheduleDTO.getName(), Endpoint.SCHEDULE_NAME);
        restManipulator.deleteDTO(scenarioDTO.getName(), Endpoint.SCENARIO_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUP_NAME);
        restManipulator.deleteDTO(resultMytabledataSetSetDTO.getName(), Endpoint.DATA_SET_NAME);
        restManipulator.deleteDTO(anotherTableSetDTO.getName(), Endpoint.DATA_SET_NAME);
        restManipulator.deleteDTO(otherTableSetDTO.getName(), Endpoint.DATA_SET_NAME);
        restManipulator.deleteDTO(mydbDataStoreDTO.getName(), Endpoint.DATA_STORE_NAME);
        restManipulator.deleteDTO(someelsedbDataStoreDTO.getName(), Endpoint.DATA_STORE_NAME);
        restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECT_NAME);

        assert (resultInitialScheduleDTO.equals(initialScheduleDTO));
        assert (resultRegularScheduleDTO.equals(regularScheduleDTO));
        assert (resultMytabledataSetSetDTO.equals(sourcetMytabledataSetSetDTO));
        assert (resultMytabledataSetSetDTOV2.equals(sourcetMytabledataSetSetDTOV2));
    }
}

package org.lakehouse.config;

import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.repository.ScenarioActRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.lakehouse.config.repository.ScriptRepository;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.service.QualityMetricsConfService;
import org.lakehouse.config.service.ScenarioActTemplateService;
import org.lakehouse.config.service.ScheduleService;
import org.lakehouse.config.test.configutation.RestManipulator;
import org.lakehouse.test.config.configuration.FileLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.FileNotFoundException;
import java.util.*;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({FileLoader.class, RestManipulator.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableJpaRepositories
public class TestWithPostgres {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    //services
    @Autowired
    ScheduleService scheduleService;
    @Autowired
    ScenarioActTemplateService scenarioActTemplateService;
    @Autowired
    QualityMetricsConfService qualityMetricsConfService;
    @Autowired
    ScheduleRepository scheduleRepository;
    @Autowired
    DataSetRepository dataSetRepository;
    @Autowired
    KafkaAdmin kafkaAdmin;

    @Autowired
    ScriptRepository scriptRepository;

    //repository
    @Autowired
    ScenarioActRepository scenarioActRepository;

    @Autowired
    DataSetSourceRepository dataSetSourceRepository;
    //test tools
    @Autowired
    FileLoader fileLoader;
    @Autowired
    RestManipulator restManipulator;

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");
    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );


    @Autowired
    private KafkaTemplate<String, ScheduleEffectiveDTO> kafkaTemplate;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    static void beforeAll() {
        kafka.start();
        postgres.start();

    }

    @AfterAll
    static void afterAll() {
        kafka.stop();
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("lakehouse.config.schedule.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

    }

    private NameSpaceDTO putNameSpaceDTO() throws Exception {
        NameSpaceDTO dto = fileLoader.loadNameSpaceDTO();

        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.PROJECTS, Endpoint.PROJECTS_NAME), NameSpaceDTO.class);
    }

    @Test
    @Order(1)
    void shouldTestNameSpaceDTO() throws Exception {
        NameSpaceDTO dto = fileLoader.loadNameSpaceDTO();
        NameSpaceDTO resultDTO = putNameSpaceDTO();
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.PROJECTS_NAME);
        assert (resultDTO.equals(dto));
    }

    private TaskExecutionServiceGroupDTO putTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();

        return ObjectMapping.stringToObject(
                restManipulator.writeAndReadDTOTest(dto.getName(), ObjectMapping.asJsonString(dto),
                        Endpoint.TASK_EXECUTION_SERVICE_GROUPS, Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME),
                TaskExecutionServiceGroupDTO.class);
    }

    @Test
    @Order(2)
    void shouldTestTaskExecutionServiceGroupDTO() throws Exception {
        TaskExecutionServiceGroupDTO dto = fileLoader.loadTaskExecutionServiceGroupDTO();
        TaskExecutionServiceGroupDTO resultDTO = putTaskExecutionServiceGroupDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        assert (resultDTO.equals(dto));
    }

    private ScenarioActTemplateDTO putScenarioDTO() throws Exception {
        ScenarioActTemplateDTO dto = fileLoader.loadScenarioDTO();
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
                        ObjectMapping.asJsonString(dto), Endpoint.SCENARIOS, Endpoint.SCENARIOS_NAME),
                ScenarioActTemplateDTO.class);
    }

    @Test
    @Order(3)
    void shouldTestScenarioDTO() throws Exception {
        TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO dto = fileLoader.loadScenarioDTO();
        ScenarioActTemplateDTO resultDTO = putScenarioDTO();
        restManipulator.deleteDTO(dto.getName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(taskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        assert (resultDTO.equals(dto));
    }

    private DataSourceDTO putDataSourceDTO(String name) throws Exception {
        DataSourceDTO dto = fileLoader.loadDataSourceDTO(name);
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SOURCES, Endpoint.DATA_SOURCES_NAME), DataSourceDTO.class);
    }

    @Test
    @Order(4)
    void shouldTestDataSourceDTO() throws Exception {
        DataSourceDTO dto = fileLoader.loadDataSourceDTO("processingdb");
        DataSourceDTO resultDTO = putDataSourceDTO("processingdb");

        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        assert (resultDTO.equals(dto));
    }


    private String loadScript(String name, String fileExt) throws Exception {
        return restManipulator.writeAndReadTextTestByKey(
                name,
                fileExt,
                fileLoader.loadModelScript(name),
                Endpoint.SCRIPT_BY_KEY,
                Endpoint.SCRIPT_BY_KEY);

    }


    @Test()
    @Order(5)
    void ShouldTestScriptConfig() throws Exception {
        String name = "client_processing";
        String estimate = fileLoader.loadModelScript(name);
        String result =
                restManipulator.writeAndReadTextTestByKey(
                        name,
                        "sql",
                        fileLoader.loadModelScript(name),
                        Endpoint.SCRIPT_BY_KEY,
                        Endpoint.SCRIPT_BY_KEY);

        assert (estimate.equals(result));
    }

    private DataSetDTO putDataSetDTO(String name) throws Exception {
        String sqlFileExt = "sql";
        try {
            loadScript(name, sqlFileExt);
        } catch (FileNotFoundException e) {
            logger.warn("File {}.{} not found", name, sqlFileExt);
        }

        DataSetDTO dto = fileLoader.loadDataSetDTO(name);
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
    }

    @Test
    @Order(5)
    void shouldTestDataSetDTO() throws Exception {
        String name = "client_processing";


        DataSourceDTO dataSourceDTO = putDataSourceDTO("processingdb");

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        DataSetDTO dto = putDataSetDTO(name);

        DataSetDTO resultDTO = ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.PROJECTS_NAME);
        assert (resultDTO.equals(dto));
    }

    @Test
    @Order(6)
    void scenarioActTemplateChange() throws Exception {

        putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO beforeWrite = fileLoader.loadScenarioActTemplateDTO();
        ScenarioActTemplateDTO afterWrite = scenarioActTemplateService.save(beforeWrite);
        assert (afterWrite.equals(beforeWrite));

        ScenarioActTemplateDTO beforeChange = afterWrite;
        TaskDTO newTask = new TaskDTO();
        newTask.setName("newTask");
        newTask.setTaskExecutionServiceGroupName("default");
        newTask.setExecutionModule("");
        newTask.setImportance("critical");
        List<TaskDTO> taskDTOS = new ArrayList<>();
        taskDTOS.addAll(beforeChange.getTasks());
        taskDTOS.add(newTask);
        beforeChange.setTasks(taskDTOS);
        ScenarioActTemplateDTO afterChange = scenarioActTemplateService.save(beforeChange);
        assert (afterChange.equals(beforeChange));
    }

    private ScheduleDTO putScheduleDTO(String name) throws Exception {
        ScheduleDTO dto = fileLoader.loadScheduleDTO(name);
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
                ObjectMapping.asJsonString(dto), Endpoint.SCHEDULES, Endpoint.SCHEDULES_NAME), ScheduleDTO.class);
    }

    @Test
    @Order(7)
    void scenarioActRepositoryFindByScheduleName() throws Exception {
        Schedule schedule = new Schedule();
        ScenarioAct sa = new ScenarioAct();
        DataSourceDTO dataSourceDTO = putDataSourceDTO("processingdb");
        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        DataSetDTO dto = putDataSetDTO("client_processing");

        schedule.setKeyName("TestScheduel");
        schedule.setDescription("TestScheduel");
        schedule.setEnabled(true);
        schedule.setIntervalExpression("********");
        schedule.setStartDateTime(DateTimeUtils.now());
        schedule.setEndDateTime(DateTimeUtils.now());
        schedule.setLastChangedDateTime(DateTimeUtils.now());
        schedule.setLastChangeNumber(1L);
        Schedule resultSchedule = scheduleRepository.save(schedule);
        sa.setSchedule(resultSchedule);
        sa.setDataSet(dataSetRepository.findAll().get(0));
        sa.setName("testScenarioAct");
        sa.setIntervalStart("");
        sa.setIntervalEnd("");
        scenarioActRepository.save(sa);

        assert (scenarioActRepository.findByScheduleKeyName(schedule.getKeyName()).size() == 1);
        // test up config version number
        ScheduleDTO scheduleDTO = scheduleService.findDtoById(schedule.getKeyName());
        ScheduleScenarioActDTO emptyScenario = new ScheduleScenarioActDTO();
        emptyScenario.setIntervalStart("1");
        emptyScenario.setIntervalEnd("2");
        emptyScenario.setDataSet(dto.getKeyName());
        scheduleDTO.setScenarioActs(List.of(emptyScenario));
        scheduleDTO.setScenarioActEdges(new ArrayList<>());
        scheduleDTO.setIntervalExpression("*****");
        ScheduleDTO resultscheduleDTO = scheduleService.save(scheduleDTO);
        assert (scheduleService.findById(resultscheduleDTO.getName()).getLastChangeNumber() == (schedule.getLastChangeNumber() + 1));

        scheduleRepository.delete(resultSchedule);
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.PROJECTS_NAME);

    }


    private ColumnDTO buildColumnDTO(
            String name,
            String description,
            String dataType,
            boolean nullable,
            Integer order,
            boolean sequence) {
        ColumnDTO result = new ColumnDTO();
        result.setName(name);
        result.setDescription(description);
        result.setDataType(dataType);
        result.setNullable(nullable);
        result.setOrder(order);
        result.setSequence(sequence);
        return result;

    }

    @Test()
    @Order(8)
    void shouldTestColumnOrdering() {
        DataSetDTO dataSetDTO = new DataSetDTO();
        ArrayList<ColumnDTO> columnDTOListEstimate = new ArrayList<>();
        columnDTOListEstimate.add(buildColumnDTO("first", "", "", true, 1, true));
        columnDTOListEstimate.add(buildColumnDTO("second", "", "", true, 2, true));
        columnDTOListEstimate.add(buildColumnDTO("Y but third by order", "", "", true, 3, true));
        columnDTOListEstimate.add(buildColumnDTO("A", "", "", true, null, true));
        columnDTOListEstimate.add(buildColumnDTO("B", "", "", true, null, true));
        columnDTOListEstimate.add(buildColumnDTO("C", "", "", true, null, true));
        columnDTOListEstimate.add(buildColumnDTO("D", "", "", true, null, true));
        columnDTOListEstimate.add(buildColumnDTO("E", "", "", true, null, true));

        List<ColumnDTO> passList = new ArrayList<>(columnDTOListEstimate);
        Collections.reverse(passList);

        dataSetDTO.setColumnSchema(passList);
        assert (columnDTOListEstimate.equals(dataSetDTO.getColumnSchema()));
    }

    @Test
    @Order(9)
    void shouldTestAllDTO() throws Exception {

        logger.info("{} {} {}", postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        // datastores
        DataSourceDTO someelsedbDataSourceDTO = putDataSourceDTO("processingdb");
        DataSourceDTO mydbDataSourceDTO = putDataSourceDTO("lakehousestorage");
        // datasets
        DataSetDTO clientProcessingDTO = putDataSetDTO("client_processing");
        DataSetDTO transactionProcessingDTO = putDataSetDTO("transaction_processing");
        DataSetDTO resultTransactionddsDTO = putDataSetDTO("transaction_dds");
        DataSetDTO sourceTransactionddsDTO = fileLoader.loadDataSetDTO(resultTransactionddsDTO.getKeyName());
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
        // schedules
        ScheduleDTO initialScheduleDTO = fileLoader.loadScheduleDTO("initial");
        ScheduleDTO regularScheduleDTO = fileLoader.loadScheduleDTO("regular");
        ScheduleDTO resultInitialScheduleDTO = putScheduleDTO("initial");
        ScheduleDTO resultRegularScheduleDTO = putScheduleDTO("regular");
        assert (resultInitialScheduleDTO.equals(initialScheduleDTO));
        assert (resultRegularScheduleDTO.equals(regularScheduleDTO));

        ScheduleEffectiveDTO scheduleEffectiveDTOExpected = fileLoader.loadScheduleEffectiveDTO();
        ScheduleEffectiveDTO scheduleEffectiveDTOResult = scheduleService
                .findEffectiveScheduleDTOById(initialScheduleDTO.getName());
        //lastChangeTime untestable
        scheduleEffectiveDTOExpected.setLastChangedDateTime(scheduleEffectiveDTOResult.getLastChangedDateTime());
        System.out.println(ObjectMapping.asJsonString(scheduleEffectiveDTOExpected));
        System.out.println(ObjectMapping.asJsonString(scheduleEffectiveDTOResult));
        assert (scheduleEffectiveDTOResult.equals(scheduleEffectiveDTOExpected));
        System.out.println(ObjectMapping.asJsonString(scheduleEffectiveDTOExpected));
        scheduleEffectiveDTOResult.getScenarioActs().stream()
                .forEach(s -> {
                    s.getTasks().forEach(taskDTO -> {
                        System.out.printf("Scenario Act name %s Task name %s%n", s.getName(), taskDTO.getName());

                    });
                    s.getDagEdges().forEach(dagEdgeDTO ->
                            System.out.printf("Scenario Act name %s Task  %s -> %s%n", s.getName(), dagEdgeDTO.getFrom(), dagEdgeDTO.getTo()));

                });

        assert (scheduleEffectiveDTOResult.getLastChangeNumber() != null);
        assert (scheduleEffectiveDTOResult.getLastChangedDateTime() != null);
        assert (scheduleEffectiveDTOResult.getScenarioActs() != null);
        assert (scheduleEffectiveDTOResult.getIntervalExpression() != null);
        assert (scheduleEffectiveDTOResult.getStartDateTime() != null);
        assert (scheduleEffectiveDTOResult.getScenarioActEdges() != null);
        //------------------------------------

        //all task in source schedule present in effective version
        initialScheduleDTO.getScenarioActs().forEach(sae -> {
            List<String> taskNamesExp = sae.getTasks().stream().map(TaskDTO::getName).toList();
            assert (scheduleEffectiveDTOResult.getScenarioActs()
                    .stream()
                    .filter(sar -> sar.getName().equals(sae.getName()))
                    .toList()
                    .get(0)
                    .getTasks()
                    .stream()
                    .map(TaskDTO::getName)
                    .toList()
                    .containsAll(taskNamesExp));
        });

        //------------------------------------
        // delete
        restManipulator.deleteDTO(resultInitialScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);
        restManipulator.deleteDTO(resultRegularScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);

        restManipulator.deleteDTO(resultAggdaily.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);

        restManipulator.deleteDTO(scenarioActTemplateDTO.getName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
                Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(someelsedbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.PROJECTS_NAME);

    }

    @Order(10)
    @Test
    void shouldTestEffectiveTask() throws Exception {
        //prepare

        scenarioActRepository.deleteAll();
        scheduleRepository.deleteAll();
        dataSetSourceRepository.deleteAll();
        dataSetRepository.deleteAll();

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        // datastores
        DataSourceDTO someelsedbDataSourceDTO = putDataSourceDTO("processingdb");
        DataSourceDTO mydbDataSourceDTO = putDataSourceDTO("lakehousestorage");
        // datasets
        DataSetDTO clientProcessingDTO = putDataSetDTO("client_processing");
        DataSetDTO transactionProcessingDTO = putDataSetDTO("transaction_processing");
        DataSetDTO resultTransactionddsDTO = putDataSetDTO("transaction_dds");
        TaskExecutionServiceGroupDTO defaultTaskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        DataSetDTO resultAggdaily = putDataSetDTO("aggregation_pay_per_client_daily_mart");
        DataSetDTO resultAggTotal = putDataSetDTO("aggregation_pay_per_client_total_mart");

        ScenarioActTemplateDTO scenarioActTemplateDTO = putScenarioDTO();
        ScheduleDTO initialScheduleDTO = putScheduleDTO("initial");

        // override template
        TaskDTO loadTaskDTOExpected = new TaskDTO();
        Map<String, String> loadExpectArgs = new HashMap<>();
        loadExpectArgs.put("spark.executor.memory", "1g");
        loadExpectArgs.put("spark.executor.cores", "2");
        loadExpectArgs.put("spark.driver.memory", "2g");
        loadExpectArgs.put("executionBody", "org.lakehouse.taskexecutor.executionmodule.body.TransformationSparkProcessorBody");
        loadTaskDTOExpected.setExecutionModuleArgs(loadExpectArgs);
        loadTaskDTOExpected.setName("load");
        loadTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        loadTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.SparkLauncherTaskProcessor");
        loadTaskDTOExpected.setImportance("critical");
        loadTaskDTOExpected.setDescription("override load");
        TaskDTO loadTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "load");
        assert (loadTaskDTO.equals(loadTaskDTOExpected));

        // not exists in template
        TaskDTO extendTaskDTOExpected = new TaskDTO();
        Map<String, String> extendTaskDTOExpectedArgs = new HashMap<>();
        extendTaskDTOExpectedArgs.put("spark.executor.memory", "5g");
        extendTaskDTOExpectedArgs.put("spark.driver.memory", "2g");
        extendTaskDTOExpectedArgs.put("executionBody", "org.lakehouse.taskexecutor.executionmodule.body.TransformationSparkProcessorBody");
        extendTaskDTOExpected.setExecutionModuleArgs(extendTaskDTOExpectedArgs);
        extendTaskDTOExpected.setName("extend");
        extendTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        extendTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.SparkLauncherTaskProcessor");
        extendTaskDTOExpected.setImportance("critical");
        extendTaskDTOExpected.setDescription("Not exists in template");
        TaskDTO extendTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "extend");
        assert (extendTaskDTO.equals(extendTaskDTOExpected));


        // exists only in template
        TaskDTO mergeTaskDTOExpected = new TaskDTO();
        Map<String, String> mergeTaskDTOExpectedArgs = new HashMap<>();
        mergeTaskDTOExpectedArgs.put("spark.executor.memory", "5g");
        mergeTaskDTOExpectedArgs.put("spark.driver.memory", "2g");
        mergeTaskDTOExpectedArgs.put("spark.driver.cores", "3");
        mergeTaskDTOExpected.setExecutionModuleArgs(mergeTaskDTOExpectedArgs);
        mergeTaskDTOExpected.setName("merge");
        mergeTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        mergeTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.datamanipulation.MergeProcessor");
        mergeTaskDTOExpected.setImportance("critical");
        mergeTaskDTOExpected.setDescription("load from remote datastore");
        TaskDTO mergeTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "merge");
        assert (mergeTaskDTO.equals(mergeTaskDTOExpected));

        // delete
        restManipulator.deleteDTO(initialScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);
        restManipulator.deleteDTO(resultAggdaily.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(scenarioActTemplateDTO.getName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
                Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(someelsedbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.PROJECTS_NAME);

    }

    @Test
    @Order(11)
    void saveQualityMetricsService() throws Exception {
        putNameSpaceDTO();
        putDataSourceDTO("lakehousestorage");
        putDataSourceDTO("processingdb");
        putDataSetDTO("client_processing");
        putDataSetDTO("transaction_processing");
        putDataSetDTO("transaction_dds");
        QualityMetricsConfDTO expected = fileLoader.loaQualityMetricsConfDTO("transaction_dds_qm");
        QualityMetricsConfDTO resulted = qualityMetricsConfService.save(expected);

        System.out.println(ObjectMapping.asJsonString(expected));
        System.out.println(ObjectMapping.asJsonString(resulted));
        assert (expected.equals(resulted));
    }
}

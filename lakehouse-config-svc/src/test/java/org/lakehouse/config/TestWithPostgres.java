package org.lakehouse.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.*;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceProperty;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.mapper.keyvalue.PropertiesIUDCase;
import org.lakehouse.config.repository.ScenarioActRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.lakehouse.config.repository.ScriptRepository;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.repository.dataset.ForeignKeyReferenceRepository;
import org.lakehouse.config.service.ScenarioActTemplateService;
import org.lakehouse.config.service.ScheduleService;
import org.lakehouse.config.service.compound.SourcesCompoundService;
import org.lakehouse.config.service.dq.QualityMetricsConfService;
import org.lakehouse.config.specifier.DataSourcePropertyKeyValueEntitySpecifier;
import org.lakehouse.config.test.configutation.RestManipulator;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.test.config.configuration.FileLoader;
import org.lakehouse.validator.config.ScheduleConfValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
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
import java.io.IOException;
import java.util.*;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ComponentScan(
        basePackages = {
                "org.lakehouse.config", "org.lakehouse.test"
        },
        basePackageClasses = {JinJavaConfiguration.class})
@Import({FileLoader.class, RestManipulator.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableJpaRepositories
public class TestWithPostgres {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    @Qualifier("jinJavaUtils")
    JinJavaUtils jinJavaUtils;

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

    @Autowired
    ForeignKeyReferenceRepository foreignKeyReferenceRepository;
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

    @Autowired
    SourcesCompoundService sourcesCompoundService;
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

    @BeforeEach
    void beforeEach(){
        foreignKeyReferenceRepository.deleteAll();
    }
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
                ObjectMapping.asJsonString(dto), Endpoint.NAME_SPACES, Endpoint.NAME_SPACES_NAME), NameSpaceDTO.class);
    }
    private DriverDTO putDriverDTO(String name) throws Exception {
        DriverDTO dto = fileLoader.loadDriverDTO(name);

        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DRIVERS, Endpoint.DRIVERS_NAME), DriverDTO.class);
    }

    @Test
    @Order(1)
    void shouldTestNameSpaceDTO() throws Exception {
        NameSpaceDTO dto = fileLoader.loadNameSpaceDTO();
        NameSpaceDTO resultDTO = putNameSpaceDTO();
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.NAME_SPACES_NAME);
        assert (resultDTO.equals(dto));
    }

    @Test
    @Order(1)
    void shouldTestDriverDTO() throws Exception {
        DriverDTO dto = fileLoader.loadDriverDTO("postgres");
        DriverDTO resultDTO = putDriverDTO(dto.getKeyName());
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DRIVERS_NAME);
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
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                        ObjectMapping.asJsonString(dto), Endpoint.SCENARIOS, Endpoint.SCENARIOS_NAME),
                ScenarioActTemplateDTO.class);
    }

    @Test
    @Order(3)
    void shouldTestScenarioDTO() throws Exception {
        TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO dto = fileLoader.loadScenarioDTO();
        ScenarioActTemplateDTO resultDTO = putScenarioDTO();
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.SCENARIOS_NAME);
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
        DriverDTO driverDTO = putDriverDTO("postgres");
        DataSourceDTO dto = fileLoader.loadDataSourceDTO("processingdb");
        DataSourceDTO resultDTO = putDataSourceDTO("processingdb");



        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(driverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        System.out.println("\nexpect--->\n" + ObjectMapping.asJsonString(dto));
        System.out.println("\nresult--->\n" + ObjectMapping.asJsonString(resultDTO));
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
        String name = "dataset-sql-model/client_processing";
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
        String sqlFilePref = "dataset-sql-model/";

        try {
            loadScript(sqlFilePref + name, sqlFileExt);
        } catch (FileNotFoundException e) {
            logger.warn("File {}{}.{} not found", sqlFilePref, name, sqlFileExt);
        }

        DataSetDTO dto = fileLoader.loadDataSetDTO(name);
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
    }



    @Test
    @Order(6)
    void shouldTestDataSetDTO() throws Exception {
        String name = "client_processing";

        DriverDTO driverDTO = putDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = putDataSourceDTO("processingdb");

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        DataSetDTO dto = putDataSetDTO(name);

        DataSetDTO resultDTO = ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(driverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        assert (resultDTO.equals(dto));
    }

    @Test
    @Order(7)
    void shouldTestDataSetDTORepeatLoad() throws Exception {
        String name = "transaction_processing";
        String dictName = "client_processing";

        DriverDTO driverDTO = putDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = putDataSourceDTO("processingdb");

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        DataSetDTO dictDto = putDataSetDTO(dictName);
        DataSetDTO dto = putDataSetDTO(name);

        // again
        DataSetDTO resultDTO = ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);


        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dictDto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(driverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        assert (resultDTO.equals(dto));
    }

    @Test
    @Order(8)
    void scenarioActTemplateChange() throws Exception {

        putTaskExecutionServiceGroupDTO();
        ScenarioActTemplateDTO beforeWrite = fileLoader.loadScenarioActTemplateDTO();
        ScenarioActTemplateDTO afterWrite = scenarioActTemplateService.save(beforeWrite);
        assert (afterWrite.equals(beforeWrite));

        ScenarioActTemplateDTO beforeChange = afterWrite;
        TaskDTO newTask = new TaskDTO();
        newTask.setName("newTask");
        newTask.setTaskExecutionServiceGroupName("default");
        newTask.setTaskProcessor("");
        newTask.setImportance("critical");
        Set<TaskDTO> taskDTOS = new HashSet<>();
        taskDTOS.addAll(beforeChange.getTasks());
        taskDTOS.add(newTask);
        beforeChange.setTasks(taskDTOS);
        ScenarioActTemplateDTO afterChange = scenarioActTemplateService.save(beforeChange);
        assert (afterChange.equals(beforeChange));
    }

    private ScheduleDTO putScheduleDTO(String name) throws Exception {
        ScheduleDTO dto = fileLoader.loadScheduleDTO(name);
        return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.SCHEDULES, Endpoint.SCHEDULES_NAME), ScheduleDTO.class);
    }

    @Test
    @Order(9)
    void scenarioActRepositoryFindByScheduleName() throws Exception {
        Schedule schedule = new Schedule();
        ScenarioAct sa = new ScenarioAct();

        DriverDTO driverDTO = putDriverDTO("postgres");
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
        scheduleDTO.setScenarioActs(Set.of(emptyScenario));
        scheduleDTO.setScenarioActEdges(new HashSet<>());
        scheduleDTO.setIntervalExpression("*****");
        ScheduleDTO resultscheduleDTO = scheduleService.save(scheduleDTO);
        assert (scheduleService.findById(resultscheduleDTO.getKeyName()).getLastChangeNumber() == (schedule.getLastChangeNumber() + 1));

        scheduleRepository.delete(resultSchedule);
        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(driverDTO.getKeyName(), Endpoint.DRIVERS_NAME);

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
    @Order(10)
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
    @Order(11)
    void shouldTestAllDTO() throws Exception {

        logger.info("{} {} {}", postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        // datastores
        DriverDTO pgDriverDTO = putDriverDTO("postgres");
        DriverDTO sparkDriverDTO = putDriverDTO("spark_iceberg");

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
                .findEffectiveScheduleDTOById(initialScheduleDTO.getKeyName());
        //lastChangeTime untestable
        scheduleEffectiveDTOExpected.setLastChangedDateTime(scheduleEffectiveDTOResult.getLastChangedDateTime());
        System.out.println("expected--->" + ObjectMapping.asJsonString(scheduleEffectiveDTOExpected));
        System.out.println("result--->" + ObjectMapping.asJsonString(scheduleEffectiveDTOResult));


        assert (scheduleEffectiveDTOResult.equals(scheduleEffectiveDTOExpected));
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
        restManipulator.deleteDTO(resultInitialScheduleDTO.getKeyName(), Endpoint.SCHEDULES_NAME);
        restManipulator.deleteDTO(resultRegularScheduleDTO.getKeyName(), Endpoint.SCHEDULES_NAME);

        restManipulator.deleteDTO(resultAggdaily.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);

        restManipulator.deleteDTO(scenarioActTemplateDTO.getKeyName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
                Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(someelsedbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(pgDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        restManipulator.deleteDTO(sparkDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);

    }

    @Order(12)
    @Test
    void shouldTestEffectiveTask() throws Exception {
        //prepare

        scenarioActRepository.deleteAll();
        scheduleRepository.deleteAll();
        dataSetSourceRepository.deleteAll();

        dataSetRepository.deleteAll();

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        // datastores
        DriverDTO pgDriverDTO = putDriverDTO("postgres");
        DriverDTO sparkDriverDTO = putDriverDTO("spark_iceberg");
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
        loadTaskDTOExpected.setTaskProcessorArgs(loadExpectArgs);
        loadTaskDTOExpected.setName("load");
        loadTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        loadTaskDTOExpected.setTaskProcessor("sparkLauncherTaskProcessor");
        loadTaskDTOExpected.setTaskProcessorBody("mergeSQLProcessorBody");
        loadTaskDTOExpected.setImportance("critical");
        loadTaskDTOExpected.setDescription("override load");
        TaskDTO loadTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getKeyName(), "transaction_dds", "load");
        //todo debug
        System.out.println("expect --->\n" + ObjectMapping.asJsonString(loadTaskDTOExpected));
        System.out.println("result --->\n" + ObjectMapping.asJsonString(loadTaskDTO));
        assert (loadTaskDTO.equals(loadTaskDTOExpected));

        // not exists in template
        TaskDTO extendTaskDTOExpected = new TaskDTO();
        Map<String, String> extendTaskDTOExpectedArgs = new HashMap<>();
        extendTaskDTOExpectedArgs.put("spark.executor.memory", "5g");
        extendTaskDTOExpectedArgs.put("spark.driver.memory", "2g");
        extendTaskDTOExpected.setTaskProcessorArgs(extendTaskDTOExpectedArgs);
        extendTaskDTOExpected.setName("extend");
        extendTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        extendTaskDTOExpected.setTaskProcessor("sparkLauncherTaskProcessor");
        extendTaskDTOExpected.setTaskProcessorBody("mergeSQLProcessorBody");
        extendTaskDTOExpected.setImportance("critical");
        extendTaskDTOExpected.setDescription("Not exists in template");
        TaskDTO extendTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getKeyName(), "transaction_dds", "extend");
        assert (extendTaskDTO.equals(extendTaskDTOExpected));


        // exists only in template
        TaskDTO mergeTaskDTOExpected = new TaskDTO();
        Map<String, String> mergeTaskDTOExpectedArgs = new HashMap<>();

        mergeTaskDTOExpected.setName("check");
        mergeTaskDTOExpected.setTaskExecutionServiceGroupName("default");
        mergeTaskDTOExpected.setTaskProcessor("dependencyCheckStateTaskProcessor");
        mergeTaskDTOExpected.setImportance("critical");
        mergeTaskDTOExpected.setDescription("Dependency check");
        TaskDTO mergeTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getKeyName(), "transaction_dds", "check");
        assert (mergeTaskDTO.equals(mergeTaskDTOExpected));

        // delete
        restManipulator.deleteDTO(initialScheduleDTO.getKeyName(), Endpoint.SCHEDULES_NAME);
        restManipulator.deleteDTO(resultAggdaily.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(scenarioActTemplateDTO.getKeyName(), Endpoint.SCENARIOS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
                Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(someelsedbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(pgDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        restManipulator.deleteDTO(sparkDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
    }

    @Test
    @Order(13)
    void mergePropertiesMapper() throws Exception {
        DataSource dataSource = new DataSource();
        dataSource.setKeyName("testDataSource");
        Mapper mapper = new Mapper();
        DataSourceProperty kveForDelete = new DataSourceProperty();
        kveForDelete.setId(1L);
        kveForDelete.setDataSource(dataSource);
        kveForDelete.setKey("deleteKey");
        kveForDelete.setValue("some value");
        DataSourceProperty kveForUpdate = new DataSourceProperty();
        kveForUpdate.setId(2L);
        kveForUpdate.setDataSource(dataSource);
        kveForUpdate.setKey("keyForUpdate");
        kveForUpdate.setValue("old value");
        Map<String,String> newKV = Map.of(
                "insertKey1", "insert value1",
                "insertKey2", "insertValue",
                kveForUpdate.getKey(),"new update value");


        List<KeyValueAbstract> exists = List.of(kveForDelete,kveForUpdate);

        KeyValueEntityMerger keyValueEntityMerger = new KeyValueEntityMerger(new DataSourcePropertyKeyValueEntitySpecifier(null,dataSource));
        PropertiesIUDCase propertiesIUDCase =  keyValueEntityMerger.splitAbstractKeyValues(exists,newKV);

        assert (propertiesIUDCase.getToBeDeleted().size() == 1);
        assert (propertiesIUDCase.getToBeInserted().size() == 2);
        assert (propertiesIUDCase.getToBeUpdated().size() == 1);
        assert (propertiesIUDCase.getToBeSaved().size() == 3);
    }
    @Test
    @Order(14)
    void testCycleGraph() throws IOException {
        ScenarioActTemplateDTO t = new ScenarioActTemplateDTO();
        DagEdgeDTO e1 = new DagEdgeDTO();
        e1.setFrom("1");
        e1.setTo("2");
        DagEdgeDTO e2 = new DagEdgeDTO();
        e2.setFrom("2");
        e2.setTo("3");
        DagEdgeDTO e3 = new DagEdgeDTO();
        e3.setFrom("3");
        e3.setTo("1");
        t.setDagEdges(Set.of(e1,e2,e3));
        List<String> stringList = ScheduleConfValidator
                .validateEdgesCycling(
                        "cycle 1,2,3",
                        ScheduleConfValidator.edgesToMap(t.getDagEdges()));
        assert (!stringList.isEmpty());

        stringList = ScheduleConfValidator
                .validateEdgesCycling(
                        "Without cycle",
                        ScheduleConfValidator.edgesToMap(fileLoader.loadScenarioActTemplateDTO().getDagEdges()));
        assert (stringList.isEmpty());
        stringList = ScheduleConfValidator
                .validateEdgesCycling(
                        "With cycle",
                        ScheduleConfValidator.edgesToMap(fileLoader.loadScenarioActTemplateDTOcycled().getDagEdges()));
        assert (!stringList.isEmpty());



    }
    private boolean isCycle(String verticeTarget,String verticeCurr, Map<String,String> edges){
        if(edges.containsKey(verticeCurr)){
            if (edges.get(verticeCurr).equals(verticeTarget)){
                return true;
            }else {
                return isCycle(verticeTarget, edges.get(verticeCurr), edges);
            }
        }else {
            return false;
        }
    }
    public static String generateDotString() {
        // Use a StringBuilder to construct the DOT string
        StringBuilder dot = new StringBuilder();

        // Start the directed graph definition
        dot.append("digraph G {\n");

        // Add graph attributes (optional)
        dot.append("  node [shape=box];\n"); // Set a default node shape

        // Define nodes and edges
        dot.append("  A -> B;\n"); // Edge from A to B
        dot.append("  A -> C;\n"); // Edge from A to C
        dot.append("  B [label=\"Node B\"];\n"); // Define node B with a custom label
        dot.append("  C [label=\"Node C\"];\n"); // Define node C with a custom label

        // End the graph definition
        dot.append("}\n");

        return dot.toString();
    }
    @Test
    @Order(15)
    public void printGraph2() throws IOException {
        ScheduleEffectiveDTO dto = fileLoader.loadScheduleEffectiveDTO();

        GraphBuilder.build2(dto);
    }
    @Test
    @Order(16)
    public void printGraph() throws IOException {
        ScheduleEffectiveDTO dto = fileLoader.loadScheduleEffectiveDTO();
        Set<DagEdgeDTO> dagEdgeDTOS = new HashSet<>();

        for (DagEdgeDTO edge:dto.getScenarioActEdges()){
            DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
            dagEdgeDTO.setFrom( edge.getFrom()+".finally");
            dagEdgeDTO.setTo( edge.getTo() + ".begin");
            dagEdgeDTOS.add(dagEdgeDTO);
        }

        for (ScheduleScenarioActEffectiveDTO a : dto.getScenarioActs()){
            for(DagEdgeDTO edge :a.getDagEdges()){
                DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
                dagEdgeDTO.setFrom(a.getName() + "." + edge.getFrom());
                dagEdgeDTO.setTo(a.getName() + "." + edge.getTo());
                dagEdgeDTOS.add(dagEdgeDTO);
            }

        }
        GraphBuilder.build(dagEdgeDTOS);

    }
    @Test
    @Order(17)
    public void testRender() throws Exception {

        String name = "client_processing";

        DriverDTO driverDTO = putDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = putDataSourceDTO("processingdb");

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        DataSetDTO dto = putDataSetDTO(name);

        DataSetDTO resultDTO = ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getKeyName(),
                ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);

        SourceConfDTO conf = sourcesCompoundService.getSourceConfDTO(name);

        restManipulator.deleteDTO(dto.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(dataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(driverDTO.getKeyName(), Endpoint.DRIVERS_NAME);

        String expected = "jdbc:postgresql://localhost:5432/postgresDB";
        String found = conf
                .getDataSources()
                .get("processingdb")
                .getService()
                .getProperties()
                .get("spark.sql.catalog.processing.url");
        assert (found.equals(expected));
    }

    @Test
    @Order(18)
    void shouldLoadDQConfig() throws Exception {
        //prepare
        scenarioActRepository.deleteAll();
        scheduleRepository.deleteAll();
        dataSetSourceRepository.deleteAll();
        dataSetRepository.deleteAll();

        NameSpaceDTO nameSpaceDTO = putNameSpaceDTO();
        // datastores
        DriverDTO pgDriverDTO = putDriverDTO("postgres");
        DriverDTO sparkDriverDTO = putDriverDTO("spark_iceberg");
        DataSourceDTO someelsedbDataSourceDTO = putDataSourceDTO("processingdb");
        DataSourceDTO mydbDataSourceDTO = putDataSourceDTO("lakehousestorage");
        // datasets
        DataSetDTO clientProcessingDTO = putDataSetDTO("client_processing");
        DataSetDTO transactionProcessingDTO = putDataSetDTO("transaction_processing");
        DataSetDTO resultTransactionddsDTO = putDataSetDTO("transaction_dds");
        TaskExecutionServiceGroupDTO defaultTaskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
        DataSetDTO resultAggdaily = putDataSetDTO("aggregation_pay_per_client_daily_mart");
        DataSetDTO resultAggTotal = putDataSetDTO("aggregation_pay_per_client_total_mart");
        //DQ
        loadScript("dq/non_zero_count", "sql");
        loadScript("dq/non_zero_count_th","sql");

        QualityMetricsConfDTO expected = fileLoader.loadQualityMetricsConfDTO("transaction_dds_qm");
        QualityMetricsConfDTO expectedOtherDataSetDQConf = fileLoader.loadQualityMetricsConfDTO("transaction_dds_qm");
        expectedOtherDataSetDQConf.setDataSetKeyName(transactionProcessingDTO.getKeyName());
        qualityMetricsConfService.save(expectedOtherDataSetDQConf);
        QualityMetricsConfDTO result = qualityMetricsConfService.save(expected);
        QualityMetricsConfDTO expected_const = qualityMetricsConfService.save(fileLoader.loadQualityMetricsConfDTO("transaction_dds_qm_const"));
        QualityMetricsConfDTO result2 = qualityMetricsConfService
                .findByDataSetKeyName(resultTransactionddsDTO.getKeyName())
                .stream()
                .filter(q-> q.getKeyName().equals("transaction_dds_qm"))
                .findFirst()
                .orElseThrow( () -> new Exception("Quality conf notfound"));
        // delete
        restManipulator.deleteDTO(expected.getKeyName(), Endpoint.QUALITY_METRICS_NAME);
        restManipulator.deleteDTO(expected_const.getKeyName(), Endpoint.QUALITY_METRICS_NAME);
        restManipulator.deleteDTO(resultAggdaily.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultAggTotal.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(resultTransactionddsDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(transactionProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(clientProcessingDTO.getKeyName(), Endpoint.DATA_SETS_NAME);
        restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(), Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
        restManipulator.deleteDTO(mydbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(someelsedbDataSourceDTO.getKeyName(), Endpoint.DATA_SOURCES_NAME);
        restManipulator.deleteDTO(nameSpaceDTO.getKeyName(), Endpoint.NAME_SPACES_NAME);
        restManipulator.deleteDTO(pgDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        restManipulator.deleteDTO(sparkDriverDTO.getKeyName(), Endpoint.DRIVERS_NAME);
        System.out.println("expected ->> \n" + ObjectMapping.asJsonString(expected));
        System.out.println("result ->> \n" +ObjectMapping.asJsonString(result));
        assert(expected.equals(result));
        assert(expected.equals(result2));

    }
    @Test
    void printEndpoints() throws JsonProcessingException {

    Arrays.asList(
        Endpoint.ROOT_API_V1_0,
        Endpoint.CONFIGS,
        Endpoint.NAME_SPACES,
        Endpoint.NAME_SPACES_NAME,
        Endpoint.TASK_EXECUTION_SERVICE_GROUPS,
        Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME,
        Endpoint.SCRIPTS,
        Endpoint.SCRIPT_BY_KEY,
        Endpoint.DATA_SETS,
        Endpoint.DATA_SETS_NAME,
        Endpoint.QUALITY_METRICS,
        Endpoint.QUALITY_METRICS_NAME,
        Endpoint.QUALITY_METRICS_BY_DATASET,
        Endpoint.DRIVERS,
        Endpoint.DRIVERS_NAME,
        Endpoint.DATA_SOURCES,
        Endpoint.DATA_SOURCES_NAME,
        Endpoint.COMPOUND,
        Endpoint.SOURCES_CONF_BY_DATASET_KEY_NAME,
        Endpoint.DATASET_MODEL_SCRIPT_BY_DATASET_KEY_NAME,
        Endpoint.SCHEDULES,
        Endpoint.SCHEDULES_NAME,
        Endpoint.EFFECTIVE_SCHEDULES_ROOT,
        Endpoint.EFFECTIVE_SCHEDULES_FROM_DT,
        Endpoint.EFFECTIVE_SCHEDULES_NAME,
        Endpoint.EFFECTIVE_SCHEDULE_SCENARIOACT_TASK,
        Endpoint.SCENARIOS,
        Endpoint.SCENARIOS_NAME,
        Endpoint.SCHEDULE,
        Endpoint.SCHEDULE_NAME,
        Endpoint.SCHEDULE_ID,
        Endpoint.TASKS,

        Endpoint.SCHEDULED_TASKS,
        Endpoint.SCHEDULED_TASKS_LOCK_BY_ID,
        Endpoint.SCHEDULED_TASKS_LOCK_ID,
        Endpoint.SCHEDULED_TASKS_LOCKS,
        Endpoint.SCHEDULED_TASKS_RELEASE,
        Endpoint.SCHEDULED_TASKS_LOCK_HEARTBEAT,
        Endpoint.SCHEDULED_TASKS_ID,
        Endpoint.STATE,
        Endpoint.STATE_DATASET,
        Endpoint.TASK_EXECUTOR,
        Endpoint.TASK_EXECUTOR_PROCESSOR,
        Endpoint.TASK_EXECUTOR_PROCESSOR_CONFIG,
        Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID
    ).stream().sorted().forEach(System.out::println);
    }


}

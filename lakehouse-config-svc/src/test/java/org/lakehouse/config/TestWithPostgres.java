package org.lakehouse.config;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.config.repository.ScriptRepository;
import org.lakehouse.test.config.configuration.FileLoader;
import org.lakehouse.config.test.configutation.RestManipulator;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.repository.DataSetRepository;
import org.lakehouse.config.repository.ScenarioActRepository;
import org.lakehouse.config.repository.ScheduleRepository;
import org.lakehouse.config.service.ScenarioActTemplateService;
import org.lakehouse.config.service.ScheduleService;
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
@Import({ FileLoader.class, RestManipulator.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableJpaRepositories
public class TestWithPostgres {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	//services
	@Autowired ScheduleService scheduleService;
	@Autowired
	ScenarioActTemplateService scenarioActTemplateService;
	@Autowired
	ScheduleRepository scheduleRepository;
	@Autowired
	DataSetRepository dataSetRepository;
	@Autowired KafkaAdmin kafkaAdmin;

	@Autowired
	ScriptRepository scriptRepository;

	//repository
	@Autowired
	ScenarioActRepository scenarioActRepository;

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
//	@Value("${lakehouse.config.schedule.kafka.producer.topic}")
	//private final  String topic ;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		//registry.add("lakehouse.config.schedule.kafka.producer.topic", topic);
		registry.add("lakehouse.config.schedule.kafka.producer.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);

	}

	private ProjectDTO putProjectDTO() throws Exception {
		ProjectDTO dto = fileLoader.loadProjectDTO();

		return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
				ObjectMapping.asJsonString(dto), Endpoint.PROJECTS, Endpoint.PROJECTS_NAME), ProjectDTO.class);
	}

	@Test
	@Order(1)
	void shouldTestProjectDTO() throws Exception {
		ProjectDTO dto = fileLoader.loadProjectDTO();
		ProjectDTO resultDTO = putProjectDTO();
		restManipulator.deleteDTO(dto.getName(), Endpoint.PROJECTS_NAME);
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

	private DataStoreDTO putDataStoreDTO(String name) throws Exception {
		DataStoreDTO dto = fileLoader.loadDataStoreDTO(name);
		return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
				ObjectMapping.asJsonString(dto), Endpoint.DATA_STORES, Endpoint.DATA_STORES_NAME), DataStoreDTO.class);
	}

	@Test
	@Order(4)
	void shouldTestDataStoreDTO() throws Exception {
		DataStoreDTO dto = fileLoader.loadDataStoreDTO("processingdb");
		DataStoreDTO resultDTO = putDataStoreDTO("processingdb");

		restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_STORES_NAME);
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
		String estimate	 = fileLoader.loadModelScript(name);
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
		}catch (FileNotFoundException e){
			logger.warn("File {}.{} not found",name,sqlFileExt);
		}

		DataSetDTO dto = fileLoader.loadDataSetDTO(name);
		return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
				ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
	}

	@Test
	@Order(5)
	void shouldTestDataSetDTO() throws Exception {
		String name = "client_processing";


		DataStoreDTO dataStoreDTO = putDataStoreDTO("processingdb");

		ProjectDTO projectDTO = putProjectDTO();
		DataSetDTO dto = putDataSetDTO(name);

		DataSetDTO resultDTO = ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
				ObjectMapping.asJsonString(dto), Endpoint.DATA_SETS, Endpoint.DATA_SETS_NAME), DataSetDTO.class);
		restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(dataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);
		assert (resultDTO.equals(dto));
	}

	private ScheduleDTO putScheduleDTO(String name) throws Exception {
		ScheduleDTO dto = fileLoader.loadScheduleDTO(name);
		return ObjectMapping.stringToObject(restManipulator.writeAndReadDTOTest(dto.getName(),
				ObjectMapping.asJsonString(dto), Endpoint.SCHEDULES, Endpoint.SCHEDULES_NAME), ScheduleDTO.class);
	}

	@Test
	@Order(6)
	void scenarioActRepositoryFindByScheduleName() throws Exception{
		Schedule schedule = new Schedule();
		ScenarioAct sa = new ScenarioAct();
		DataStoreDTO dataStoreDTO = putDataStoreDTO("processingdb");
		ProjectDTO projectDTO = putProjectDTO();
		DataSetDTO dto = putDataSetDTO("client_processing");

		schedule.setName("TestScheduel");
		schedule.setDescription("TestScheduel");
		schedule.setEnabled(true);
		schedule.setIntervalExpression("********");
		schedule.setStartDateTime(DateTimeUtils.now());
		schedule.setEndDateTime(DateTimeUtils.now());
		schedule.setLastChangedDateTime(DateTimeUtils.now());
		schedule.setLastChangeNumber(1L);
		Schedule resultSchedule =  scheduleRepository.save(schedule);
		sa.setSchedule(resultSchedule);
		sa.setDataSet(dataSetRepository.findAll().get(0));
		sa.setName("testScenarioAct");
		scenarioActRepository.save(sa);
		
		assert(	scenarioActRepository.findByScheduleName(schedule.getName()).size() ==1);
		// test up config version number
		ScheduleDTO scheduleDTO = scheduleService.findDtoById(schedule.getName());
		scheduleDTO.setScenarioActs(new ArrayList<>());
		scheduleDTO.setScenarioActEdges(new ArrayList<>());
		scheduleDTO.setIntervalExpression("*****");
		ScheduleDTO resultscheduleDTO = scheduleService.save(scheduleDTO);
		assert (scheduleService.findById(resultscheduleDTO.getName()).getLastChangeNumber() == (schedule.getLastChangeNumber()+1));

		scheduleRepository.delete(resultSchedule);;
		restManipulator.deleteDTO(dto.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(dataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);
	
	}


	private ColumnDTO buildColumnDTO(
			String name,
			String description ,
			String dataType,
			boolean nullable,
			Integer order,
			boolean sequence){
		ColumnDTO result = new ColumnDTO();
		result.setName(name);
		result.setDescription ( description);
		result.setDataType( dataType);
		result.setNullable( nullable);
		result.setOrder( order);
		result.setSequence(sequence);
		return result;

	}

	@Test()
	@Order(7)
	void shouldTestColumnOrdering(){
		DataSetDTO dataSetDTO = new DataSetDTO();
		ArrayList<ColumnDTO> columnDTOListEstimate = new ArrayList<>();
		columnDTOListEstimate.add(buildColumnDTO("first", "","",true,1,true));
		columnDTOListEstimate.add(buildColumnDTO("second", "","",true,2,true));
		columnDTOListEstimate.add(buildColumnDTO("Y but third by order", "","",true,3,true));
		columnDTOListEstimate.add(buildColumnDTO("A", "","",true,null,true));
		columnDTOListEstimate.add(buildColumnDTO("B", "","",true,null,true));
		columnDTOListEstimate.add(buildColumnDTO("C", "","",true,null,true));
		columnDTOListEstimate.add(buildColumnDTO("D", "","",true,null,true));
		columnDTOListEstimate.add(buildColumnDTO("E", "","",true,null,true));

        List<ColumnDTO> passList = new ArrayList<>(columnDTOListEstimate);
		Collections.reverse(passList);

		dataSetDTO.setColumnSchema(passList);
		assert (columnDTOListEstimate.equals(dataSetDTO.getColumnSchema()));
	}
	@Test
	@Order(8)
	void shouldTestAllDTO() throws Exception {

        logger.info("{} {} {}",postgres.getJdbcUrl(),postgres.getUsername(), postgres.getPassword());

		ProjectDTO projectDTO = putProjectDTO();
		// datastores
		DataStoreDTO someelsedbDataStoreDTO = putDataStoreDTO("processingdb");
		DataStoreDTO mydbDataStoreDTO = putDataStoreDTO("lakehousestorage");
		// datasets
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
		assert(resultAggdaily.equals(sourceAggdaily));
		assert(resultAggTotal.equals(sourceAggTotal));
		assert(resultTransactionddsDTO.equals(sourceTransactionddsDTO));
		assert(resultTransactionddsDTOV2.equals(sourceTransactionddsDTOV2));

		TaskExecutionServiceGroupDTO defaultTaskExecutionServiceGroupDTO = putTaskExecutionServiceGroupDTO();
		ScenarioActTemplateDTO scenarioActTemplateDTO = putScenarioDTO();
		// schedules
		ScheduleDTO initialScheduleDTO = fileLoader.loadScheduleDTO("initial");
		ScheduleDTO regularScheduleDTO = fileLoader.loadScheduleDTO("regular");
		ScheduleDTO resultInitialScheduleDTO = putScheduleDTO("initial");
		ScheduleDTO resultRegularScheduleDTO = putScheduleDTO("regular");
		assert (resultInitialScheduleDTO.equals(initialScheduleDTO));
		assert (resultRegularScheduleDTO.equals(regularScheduleDTO));

		ScheduleEffectiveDTO scheduleEffectiveDTO = scheduleService
				.findEffectiveScheduleDTOById(initialScheduleDTO.getName());
		assert ( //  sum total of merge template with direct tasks
				scheduleEffectiveDTO
				.getScenarioActs()
				.stream()
				.mapToInt(s -> s.getTasks().size())
				.sum() == 24);
		assert ( //extended task in schedule scenario
				scheduleEffectiveDTO
				.getScenarioActs()
				.stream()
				.mapToInt(sa -> sa.getTasks()
						.stream()
						.filter(t-> t.getName().equals("extend"))
						.toList()
						.size())
				.sum() == 1);
		System.out.println(ObjectMapping.asJsonString(scheduleEffectiveDTO));
		ScheduleEffectiveDTO sef = scheduleService.findEffectiveScheduleDTOById(initialScheduleDTO.getName());
		sef.getScenarioActs().stream()
				.forEach(s -> {
					s.getTasks().forEach(taskDTO -> {
						System.out.printf("Scenario Act name %s Task name %s%n", s.getName(), taskDTO.getName());

						});
					s.getDagEdges().forEach(dagEdgeDTO ->
							System.out.printf("Scenario Act name %s Task  %s -> %s%n",s.getName(),dagEdgeDTO.getFrom(),dagEdgeDTO.getTo()));

				});

		assert (sef.getLastChangeNumber() !=null);
		assert (sef.getLastChangedDateTime() !=null);
		assert (sef.getScenarioActs() !=null);
		assert (sef.getIntervalExpression() !=null);
		assert (sef.getStartDateTime() !=null);
		assert (sef.getScenarioActEdges() != null);
		//------------------------------------

		//all task in source schedule present in effective version
		initialScheduleDTO.getScenarioActs().forEach( sae -> {
			List<String> taskNamesExp = sae.getTasks().stream().map(TaskDTO::getName).toList();
			assert (scheduleEffectiveDTO.getScenarioActs()
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

		restManipulator.deleteDTO(resultAggdaily.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(resultAggTotal.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(resultTransactionddsDTO.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(transactionProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(clientProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);

		restManipulator.deleteDTO(scenarioActTemplateDTO.getName(), Endpoint.SCENARIOS_NAME);
		restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
				Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
		restManipulator.deleteDTO(mydbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(someelsedbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);

	}

	@Order(9)
	@Test
	void shouldTestEffectiveTask() throws Exception {
		//prepare
		ProjectDTO projectDTO = putProjectDTO();
		// datastores
		DataStoreDTO someelsedbDataStoreDTO = putDataStoreDTO("processingdb");
		DataStoreDTO mydbDataStoreDTO = putDataStoreDTO("lakehousestorage");
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
		Map<String,String> loadExpectArgs = new HashMap<>();
		loadExpectArgs.put("spark.executor.memory", "1gb");
		loadExpectArgs.put( "spark.executor.cores", "2");
		loadExpectArgs.put( "spark.driver.memory", "2gb");
		loadTaskDTOExpected.setExecutionModuleArgs(loadExpectArgs);
		loadTaskDTOExpected.setName("load");
		loadTaskDTOExpected.setTaskExecutionServiceGroupName("default");
		loadTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.SparkTaskProcessor");
		loadTaskDTOExpected.setImportance("critical");
		loadTaskDTOExpected.setDescription("override load");
		TaskDTO loadTaskDTO   = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "load");
		assert (loadTaskDTO.equals(loadTaskDTOExpected));

		// not exists in template
		TaskDTO extendTaskDTOExpected = new TaskDTO();
		Map<String,String> extendTaskDTOExpectedArgs = new HashMap<>();
		extendTaskDTOExpectedArgs.put("spark.executor.memory","5gb");
		extendTaskDTOExpectedArgs.put("spark.driver.memory","2gb");
		extendTaskDTOExpected.setExecutionModuleArgs(extendTaskDTOExpectedArgs);
		extendTaskDTOExpected.setName("extend");
		extendTaskDTOExpected.setTaskExecutionServiceGroupName("default");
		extendTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.SparkTaskProcessor");
		extendTaskDTOExpected.setImportance("critical");
		extendTaskDTOExpected.setDescription("Not exists in template");
		TaskDTO extendTaskDTO = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "extend");
		assert (extendTaskDTO.equals(extendTaskDTOExpected));


		// exists only in template
		TaskDTO mergeTaskDTOExpected = new TaskDTO();
		Map<String,String> mergeTaskDTOExpectedArgs = new HashMap<>();
		mergeTaskDTOExpectedArgs.put("spark.executor.memory", "5gb");
		mergeTaskDTOExpectedArgs.put("spark.driver.memory", "2gb");
		mergeTaskDTOExpectedArgs.put("spark.driver.cores", "3");
		mergeTaskDTOExpected.setExecutionModuleArgs(mergeTaskDTOExpectedArgs);
		mergeTaskDTOExpected.setName("merge");
		mergeTaskDTOExpected.setTaskExecutionServiceGroupName("default");
		mergeTaskDTOExpected.setExecutionModule("org.lakehouse.taskexecutor.executionmodule.datamanipulation.MergeProcessor");
		mergeTaskDTOExpected.setImportance("critical");
		mergeTaskDTOExpected.setDescription("load from remote datastore");
		TaskDTO mergeTaskDTO  = scheduleService.getEffectiveTaskDTO(initialScheduleDTO.getName(), "transaction_dds", "merge");
		assert (mergeTaskDTO.equals(mergeTaskDTOExpected));

		// delete
		restManipulator.deleteDTO(initialScheduleDTO.getName(), Endpoint.SCHEDULES_NAME);
		restManipulator.deleteDTO(resultAggdaily.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(resultAggTotal.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(resultTransactionddsDTO.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(transactionProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(clientProcessingDTO.getName(), Endpoint.DATA_SETS_NAME);
		restManipulator.deleteDTO(scenarioActTemplateDTO.getName(), Endpoint.SCENARIOS_NAME);
		restManipulator.deleteDTO(defaultTaskExecutionServiceGroupDTO.getName(),
				Endpoint.TASK_EXECUTION_SERVICE_GROUPS_NAME);
		restManipulator.deleteDTO(mydbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(someelsedbDataStoreDTO.getName(), Endpoint.DATA_STORES_NAME);
		restManipulator.deleteDTO(projectDTO.getName(), Endpoint.PROJECTS_NAME);

	}

}

package org.lakehouse.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.factory.StateFactory;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.repository.DataSetStateRepository;
import org.lakehouse.state.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableJpaRepositories
public class DataSetStateTest {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	StateService stateService;



	@Autowired
	DataSetStateRepository dataSetStateRepository;

	@Autowired
	StateFactory stateFactory;
	@SuppressWarnings("resource")
	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
			.withUsername("name").withPassword("password");
	@Container
	static final KafkaContainer kafka = new KafkaContainer(
			DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
	);


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

	private void prepareHistorySet() throws IOException {
		dataSetStateRepository.deleteAll();
		ObjectMapper objectMapper = new ObjectMapper();
		List<DataSetState> l =
				objectMapper
						.readValue(
								getClass()
										.getClassLoader()
										.getResource("states.json"),
								new TypeReference<List<DataSetStateDTO>>(){})
						.stream()
						.map(StateMapper::getState)
						.toList();

		l.forEach(state -> System.out.println(state.getIntervalStartDateTime()));
		dataSetStateRepository.saveAll(l);
		logger.info("Prepared History state set");
		dataSetStateRepository
				.findAll()
				.stream()
				.sorted(Comparator.comparing(DataSetState::getId))
				.forEach(dataSetState -> logger.info("stored {}",dataSetState));
	}

	@Test
	@Order(1)
	void addNewNotExistingState() throws Exception {
		/*
		 * last /--------------------------------/
		 * curr                                  /--------------------------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-10T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-11T00:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);

		int count = dataSetStateRepository.findAll().size();
		stateService.save(newState);
		assert (count+1 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
	}
	@Test
	@Order(2)
	void updateState() throws Exception {
		/*
		 * last /--------------------------------/
		 * curr /--------------------------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-09T00:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);

		int count = dataSetStateRepository.findAll().size();
		stateService.save(newState);
		assert (count == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
	}
	@Test
	@Order(3)
	void splitLeftState() throws Exception {
		/*
		 * last /------------------------/
		 * curr /--------------------------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T01:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);

		int count = dataSetStateRepository.findAll().size();
		stateService.save(newState);
		assert (count + 1 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getIntervalStartDateTime().isEqual(newState.getIntervalEndDateTime())).toList().size() == 1);

	}
	@Test
	@Order(4)
	void splitRightState() throws Exception {
		/*
		 * last /------------------------/
		 * curr        /---------------- /
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T17:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-09T00:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);

		int count = dataSetStateRepository.findAll().size();
		stateService.save(newState);
		assert (count + 1 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getIntervalEndDateTime().isEqual(newState.getIntervalStartDateTime())).toList().size() == 1);
	}


	@Test
	@Order(4)
	void overwriteState() throws Exception {
		/*
		 * last      /--------------/
		 * curr /----------------------- /
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-01T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-31T00:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);


		stateService.save(newState);
		assert (2 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(
				dataSetState -> dataSetState.getStatus().equals(newState.getStatus())
							&& dataSetState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime())
							&& dataSetState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime())
				).toList().size() == 1);
	}
	@Test
	@Order(5)
	void splitMiddleState() throws Exception {
		/*
		 * last /----------------------- /
		 * curr     /--------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-02T08:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-02T16:00Z"));
		newState.setStatus(Status.DataSet.RUNNING.label);

		int count = dataSetStateRepository.findAll().size();

		stateService.save(newState);
		assert (count + 2 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(
				dataSetState -> dataSetState.getStatus().equals(newState.getStatus())
						&& dataSetState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime())
						&& dataSetState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime()))
				.toList().size() == 1);
	}



	private List<DataSetState> feelGaps(List<DataSetState> dataSetStates){
		List<DataSetState> result = new ArrayList<>();
		List<DataSetState> dataSetStatesSorted = stateFactory.sortStates(dataSetStates);
		for (int i=0; i < dataSetStatesSorted.size(); i++){
			DataSetState curr = dataSetStates.get(i);
			if (i > 0){
				DataSetState prev = dataSetStatesSorted.get(i-1);

				if(prev.getIntervalEndDateTime().isBefore(curr.getIntervalStartDateTime())){
					DataSetState gap = new DataSetState();
					gap.setIntervalStartDateTime(prev.getIntervalEndDateTime());
					gap.setIntervalEndDateTime(curr.getIntervalStartDateTime());
					gap.setStatus(null);
					gap.setDataSetKeyName(null);
					result.add(gap);
				}
				result.add(curr);
			}
			else result.add(curr);
		}
		return result;
	}


	@Test
	@Order(6)
	void getState() throws Exception {
		prepareHistorySet();
		String dataSetKeyName = "test1";
		OffsetDateTime intervalStartDateTime = DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-04T01:00Z");
		OffsetDateTime intervalEndDateTime   = DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T02:00Z");

		List<DataSetState> current = stateFactory.sortStates(
				dataSetStateRepository
						.findIntersection(
								dataSetKeyName,
								intervalStartDateTime,
								intervalEndDateTime));

		dataSetStateRepository.delete(current.get(0));
		dataSetStateRepository.delete(current.get(current.size()-3));
		dataSetStateRepository.delete(current.get(current.size()-1));


		if (intervalEndDateTime.isAfter(current.get(current.size()-1).getIntervalStartDateTime())){
			DataSetState gap = new DataSetState();
			gap.setDataSetKeyName(dataSetKeyName);
			gap.setIntervalStartDateTime(current.get(current.size()-1).getIntervalEndDateTime());
			gap.setIntervalEndDateTime(intervalEndDateTime);
			gap.setStatus(null);
			current.add(gap);
		}

		if (intervalStartDateTime.isBefore(current.get(0).getIntervalStartDateTime())){
			DataSetState gap = new DataSetState();
			gap.setDataSetKeyName(dataSetKeyName);
			gap.setIntervalStartDateTime(intervalStartDateTime);
			gap.setIntervalEndDateTime(current.get(0).getIntervalEndDateTime());
			gap.setStatus(null);
			current.add(gap);
		}

		current = feelGaps(current);

		current.forEach(state -> logger.info(">>---> \n{}",state));

	}
}

package org.lakehouse.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.factory.StateFactory;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.repository.DataSetStateRepository;
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
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/*
@ComponentScan(basePackageClasses = {
		org.lakehouse.state.entity.State.class,
		org.lakehouse.state.service.StateService.class,
		org.lakehouse.state.repository.StateRepository.class})
*/

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnableJpaRepositories
public class DataSetStateTest {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());


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
	private void testMerge(DataSetState newState){
		List<DataSetState> current = dataSetStateRepository.findIntersection(newState.getIntervalStartDateTime(),newState.getIntervalEndDateTime());
		List<DataSetState> merged = stateFactory.merge(newState,current);
		merged.forEach(dataSetState -> logger.info("merged {}",dataSetState));
		dataSetStateRepository.saveAll(merged);
		dataSetStateRepository.findAll().forEach(dataSetState -> logger.info("stored {}",dataSetState));
	}
	@Test
	@Order(1)
	void addNewState() throws Exception {
		/*
		 * last /--------------------------------/
		 * curr                                  /--------------------------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-10T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-11T00:00Z"));
		newState.setStatus("FAILED");

		int count = dataSetStateRepository.findAll().size();
		testMerge(newState);
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
		newState.setStatus("FAILED");

		int count = dataSetStateRepository.findAll().size();
		testMerge(newState);
		assert (count == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
	}	@Test
	@Order(3)
	void splitLeftState() throws Exception {
		/*
		 * last /--------------------------------------------/
		 * curr /--------------------------------/
		 * */
		prepareHistorySet();
		DataSetState newState = new DataSetState();
		newState.setDataSetKeyName("test1");
		newState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T00:00Z"));
		newState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ("2025-01-08T01:00Z"));
		newState.setStatus("FAILED");

		int count = dataSetStateRepository.findAll().size();
		testMerge(newState);
		assert (count + 1 == dataSetStateRepository.findAll().size());
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getStatus().equals(newState.getStatus())).toList().size() == 1);
		assert (dataSetStateRepository.findAll().stream().filter(dataSetState -> dataSetState.getIntervalStartDateTime().isEqual(newState.getIntervalEndDateTime())).toList().size() == 1);
	}
}

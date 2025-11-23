package org.lakehouse.state;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.exception.LockedStateRuntimeException;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
                                new TypeReference<List<DataSetStateDTO>>() {
                                })
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
                .forEach(dataSetState -> logger.info("stored {}", dataSetState));
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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-10T00:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-11T00:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);

        int count = dataSetStateRepository.findAll().size();
        stateService.save(newState);
        assert (count + 1 == dataSetStateRepository.findAll().size());
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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-08T00:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-09T00:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);
        newState.setLockSource("test");

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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-08T00:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-08T01:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);

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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-08T17:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-09T00:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);

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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-31T00:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);


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
        newState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T08:00Z"));
        newState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-02T16:00Z"));
        newState.setStatus(Status.DataSet.LOCKED.label);

        int count = dataSetStateRepository.findAll().size();

        stateService.save(newState);
        assert (count + 2 == dataSetStateRepository.findAll().size());
        assert (dataSetStateRepository.findAll().stream().filter(
                        dataSetState -> dataSetState.getStatus().equals(newState.getStatus())
                                && dataSetState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime())
                                && dataSetState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime()))
                .toList().size() == 1);
    }


    @Test
    @Order(6)
    void getState() throws Exception {
        prepareHistorySet();

        String dataSetKeyName = "test1";
        OffsetDateTime intervalStartDateTime = DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-03T00:00Z");
        OffsetDateTime intervalFailedStateStartDateTime = intervalStartDateTime.plusDays(1);
        OffsetDateTime intervalMiddleRowStateStartDateTime = intervalStartDateTime.plusDays(2);
        OffsetDateTime intervalEndDateTime = intervalStartDateTime.plusDays(5);

        DataSetState failedState = new DataSetState();
        failedState.setDataSetKeyName(dataSetKeyName);
        failedState.setStatus(Status.DataSet.LOCKED.label);
        failedState.setIntervalStartDateTime(intervalFailedStateStartDateTime);
        failedState.setIntervalEndDateTime(intervalFailedStateStartDateTime.plusDays(1));
        failedState.setLockSource("testSchedule.testAct.testTask");
        // change status one interval to failed
        stateService.save(failedState);


        List<DataSetState> dataSetStates = dataSetStateRepository.findAll().stream().filter(state -> state.getDataSetKeyName().equals(dataSetKeyName)).toList();
        //delete first interval for test first interval gap
        dataSetStateRepository.delete(dataSetStates.stream().filter(state -> state.getIntervalStartDateTime().isEqual(intervalStartDateTime)).toList().get(0));
        //delete last interval for test last interval gap
        dataSetStateRepository.delete(dataSetStates.stream().filter(state -> state.getIntervalEndDateTime().isEqual(intervalEndDateTime)).toList().get(0));

        // delete one of middle intervals for test gap in middle
        dataSetStateRepository.delete(dataSetStates.stream().filter(state -> state.getIntervalStartDateTime().isEqual(intervalMiddleRowStateStartDateTime)).toList().get(0));

        dataSetStateRepository.findIntersection(dataSetKeyName, intervalStartDateTime, intervalEndDateTime).forEach(state -> System.out.println("intersection " + state.toString()));

        DataSetStateResponseDTO dataSetStateResponseDTO = stateService.getStateByInterval(dataSetKeyName, intervalStartDateTime, intervalEndDateTime);
        dataSetStateResponseDTO
                .getWrongStates()
                .stream()
                .forEach(state -> System.out.println("wrongStates " + state.toString()));

        assert (dataSetStateResponseDTO
                .getWrongStates()
                .size() == 4
        );

        assert (dataSetStateResponseDTO
                .getWrongStates()
                .stream()
                .filter(i ->
                        DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalStartDateTime()).isEqual(failedState.getIntervalStartDateTime()) &&
                                DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalEndDateTime()).isEqual(failedState.getIntervalEndDateTime()) &&
                                i.getStatus().equals(failedState.getStatus())
                ).toList().size() == 1
        );
        assert (dataSetStateResponseDTO
                .getWrongStates()
                .stream()
                .filter(i ->
                        DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalStartDateTime()).isEqual(intervalStartDateTime) &&
                                DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalEndDateTime()).isEqual(intervalStartDateTime.plusDays(1)) &&
                                i.getStatus() == null
                ).toList().size() == 1
        );
        assert (dataSetStateResponseDTO
                .getWrongStates()
                .stream()
                .filter(i ->
                        DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalStartDateTime()).isEqual(intervalMiddleRowStateStartDateTime) &&
                                DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalEndDateTime()).isEqual(intervalMiddleRowStateStartDateTime.plusDays(1)) &&
                                i.getStatus() == null
                ).toList().size() == 1
        );
        assert (dataSetStateResponseDTO
                .getWrongStates()
                .stream()
                .filter(i ->
                        DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalStartDateTime()).isEqual(intervalEndDateTime.minusDays(1)) &&
                                DateTimeUtils.parseDateTimeFormatWithTZ(i.getIntervalEndDateTime()).isEqual(intervalEndDateTime) &&
                                i.getStatus() == null
                ).toList().size() == 1
        );
    }

    @Test
    @Order(7)
    void testStateHashValueNegative() throws Exception {
        dataSetStateRepository.deleteAll();
        DataSetStateDTO fromFirstClient = new DataSetStateDTO();
        fromFirstClient.setDataSetKeyName("DataSetTest");
        fromFirstClient.setStatus(Status.DataSet.LOCKED.label);
        fromFirstClient.setIntervalStartDateTime("2025-01-01T00:00:00z");
        fromFirstClient.setIntervalEndDateTime("2025-01-02T00:00:00z");
        fromFirstClient.setLockSource(String.valueOf(Objects.hashCode("first")));

        DataSetStateDTO fromSecondClient = new DataSetStateDTO();
        fromSecondClient.setDataSetKeyName(fromFirstClient.getDataSetKeyName());
        fromSecondClient.setStatus(Status.DataSet.SUCCESS.label);
        fromSecondClient.setLockSource(String.valueOf(Objects.hashCode("second")));
        fromSecondClient.setIntervalStartDateTime(fromFirstClient.getIntervalStartDateTime());
        fromSecondClient.setIntervalEndDateTime(fromFirstClient.getIntervalEndDateTime());
        stateService.save(StateMapper.getState(fromFirstClient));
        // expect RuntimeException
        boolean OK;
        try {
            stateService.save(StateMapper.getState(fromSecondClient));
            OK = false;
        } catch (LockedStateRuntimeException e) {
            OK = true;
        }
        assert (OK);

    }

    @Test
    @Order(8)
    void testStateHashValuePositive() throws Exception {
        dataSetStateRepository.deleteAll();
        String lockSource = String.valueOf(Objects.hashCode("single"));

        DataSetStateDTO fromFirstClient = new DataSetStateDTO();
        fromFirstClient.setDataSetKeyName("DataSetTest");
        fromFirstClient.setStatus(Status.DataSet.LOCKED.label);
        fromFirstClient.setIntervalStartDateTime("2025-01-01T00:00:00z");
        fromFirstClient.setIntervalEndDateTime("2025-01-02T00:00:00z");
        fromFirstClient.setLockSource(lockSource);

        DataSetStateDTO fromSecondClient = new DataSetStateDTO();
        fromSecondClient.setDataSetKeyName(fromFirstClient.getDataSetKeyName());
        fromSecondClient.setStatus(Status.DataSet.SUCCESS.label);
        fromSecondClient.setLockSource(fromFirstClient.getLockSource());
        fromSecondClient.setIntervalStartDateTime(fromFirstClient.getIntervalStartDateTime());
        fromSecondClient.setIntervalEndDateTime(fromFirstClient.getIntervalEndDateTime());

        stateService.save(StateMapper.getState(fromFirstClient));
        // expect RuntimeException
        boolean OK;
        try {
            stateService.save(StateMapper.getState(fromSecondClient));
            OK = true;
        } catch (LockedStateRuntimeException e) {
            OK = false;
        }
        assert (OK);

    }

    @Test
    @Order(9)
    void testStateCollapse() throws Exception {
        dataSetStateRepository.deleteAll();
        String lockSource = "collapse";

        DataSetState start = new DataSetState();
        start.setDataSetKeyName("DataSetTest");
        start.setStatus(Status.DataSet.SUCCESS.label);
        start.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2024-12-31T00:00:00z"));
        start.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z"));
        start.setLockSource(lockSource);
        stateService.save(start);
        for (long i = 1; i <= 35; i++) {
            DataSetState state = new DataSetState();
            state.setDataSetKeyName(start.getDataSetKeyName());
            state.setStatus(Status.DataSet.SUCCESS.label);
            state.setIntervalStartDateTime(start.getIntervalStartDateTime().plusDays(i));
            state.setIntervalEndDateTime(start.getIntervalEndDateTime().plusDays(i));
            state.setLockSource(lockSource);
            stateService.save(state);
        }
        DataSetState month = new DataSetState();
        month.setDataSetKeyName(start.getDataSetKeyName());
        month.setStatus(Status.DataSet.LOCKED.label);
        month.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-01-01T00:00:00z"));
        month.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ("2025-02-01T00:00:00z"));
        month.setLockSource(lockSource);
        stateService.save(month);

        dataSetStateRepository
                .findAll()
                .stream()
                .sorted(
                        Comparator.comparing(DataSetState::getIntervalStartDateTime)
                                .thenComparing(DataSetState::getIntervalEndDateTime))
                .forEach(System.out::println);

    }

}
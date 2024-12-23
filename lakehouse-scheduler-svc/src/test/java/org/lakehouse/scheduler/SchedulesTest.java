package org.lakehouse.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.cli.api.constant.Endpoint;
import org.lakehouse.cli.api.dto.configs.ScenarioActTemplateDTO;
import org.lakehouse.cli.api.dto.configs.ScheduleDTO;
import org.junit.jupiter.api.*;
import org.lakehouse.cli.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.cli.api.utils.DateTimeUtils;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.lakehouse.scheduler.repository.ScheduleInstanceLastBuildRepository;
import org.lakehouse.scheduler.service.ScheduleInstanceLastBuildService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ComponentScan(basePackages = {"org.lakehouse.config.rest.client"})
//@ComponentScan
/**ComponentScan(basePackages = {"org.lakehouse"}, excludeFilters={
        @ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE, value= LakehouseSchedulerApp.class)})
 */
//@EnableJpaRepositories
public class SchedulesTest {
//services
    @Autowired
ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;

   @Autowired ClientApi clientApi;

    //repository

    @Autowired
    ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;

    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired MockRestServiceServer server;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

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

    public ScenarioActTemplateDTO loadScenarioActTemplateDTO() throws IOException {
        return objectMapper.readValue(new File("../lakehouse-config-svc/demo/scenario-act-templates/default.json"), ScenarioActTemplateDTO.class);
    }


    public ScheduleEffectiveDTO loadScheduleDTO() throws IOException {
        return objectMapper.readValue(new File("../lakehouse-config-svc/demo/schedules_effective/initial.json"), ScheduleEffectiveDTO.class);
    }


    @Test
    public void fullpipeline() throws Exception {

        List<ScheduleEffectiveDTO> scheduleDTOsExpect = new ArrayList<>();
        scheduleDTOsExpect.add(loadScheduleDTO());
        OffsetDateTime scheduleChangeDateTimeStr = DateTimeUtils.now();
        server.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/%s", Endpoint.EFFECTIVE_SCHEDULES_FROM_DT, scheduleChangeDateTimeStr)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(scheduleDTOsExpect), MediaType.APPLICATION_JSON));
        System.out.println("scenario is loaded");


        List<ScheduleEffectiveDTO> scheduleEffectiveDTOS =
                clientApi.getScheduleEffectiveDTOList(scheduleChangeDateTimeStr);

        ScenarioActTemplateDTO scenarioActTemplateDTO = loadScenarioActTemplateDTO();

        server.expect(ExpectedCount.manyTimes(),
                        requestTo(String.format("%s/%s", Endpoint.SCENARIOS, scenarioActTemplateDTO.getName())))
                .andRespond(withSuccess(objectMapper.writeValueAsString(scenarioActTemplateDTO), MediaType.APPLICATION_JSON));

        System.out.println("scenario is loaded");

        scheduleInstanceLastBuildService.findAndRegisterNewSchedules(scheduleEffectiveDTOS);
        assert (scheduleInstanceLastBuildRepository.findAll().size() ==1);
    }

}

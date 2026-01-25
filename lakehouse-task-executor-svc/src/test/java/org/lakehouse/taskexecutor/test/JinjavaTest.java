package org.lakehouse.taskexecutor.test;

import com.hubspot.jinjava.Jinjava;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskConfigBuildException;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskProcessorConfigFactory;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JinjavaTest {
    @Autowired
    @Qualifier("jinjava")
    Jinjava jinjava;

    @Test
    @Order(1)
    public void testJinjaAddDay() {
        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays('" + targetDateTime.format(dateTimeFormatter) + "', 10)}}";
        String renderedTemplate = jinjava.render(template, new HashMap<>());
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }

    @Test
    @Order(2)
    public void testJinjaAddDay2() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }


    @Test
    @Order(3)
    public void testJinjaContextReplacement2() {

        String targetDateTimeStr = "2025-06-12T16:03:00.435821544+03:00";
        String template = "{{ " + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + " }}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTimeStr));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTimeStr));
    }

    @Test
    @Order(2)
    public void testJinjaAddMonths() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ addmonths(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, String> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusMonths(10).format(dateTimeFormatter)));
    }


    @Test
    @Order(6)
    public void testLoadDataSet() throws IOException, TaskConfigBuildException {
        ConfigRestClientApiTest configRestClientApiTest = new ConfigRestClientApiTest();
        ScheduledTaskLockDTO scheduledTaskLockDTO = new ScheduledTaskLockDTO();
        scheduledTaskLockDTO.setLockId(1L);
        scheduledTaskLockDTO.setServiceId("testService");
        ScheduledTaskDTO taskDTO = configRestClientApiTest
                .getScheduleEffectiveDTO(null)
                .getScenarioActs()
                .stream()
                .filter(a-> a.getDataSet().equals("transaction_dds"))
                .flatMap(s-> s.getTasks()
                        .stream()
                        .filter(t-> t.getName().equals("load"))
                        .map(t-> {
                            ScheduledTaskDTO st = new ScheduledTaskDTO();
                            st.setStatus(Status.Task.NEW);
                            st.setId(1L);
                            st.setScenarioActKeyName("testAct");
                            st.setTargetDateTime(DateTimeUtils.nowStr());
                            st.setIntervalStartDateTime(DateTimeUtils.nowStr());
                            st.setIntervalEndDateTime(DateTimeUtils.nowStr());
                            st.setDataSetKeyName("transaction_dds");
                            return st;
                        })
                ).toList()
                .get(0);
        scheduledTaskLockDTO.setScheduledTaskEffectiveDTO(taskDTO);
        TaskProcessorConfigFactory tpcf = new TaskProcessorConfigFactory(
                new ConfigRestClientApiTest(),  jinjava);
        TaskProcessorConfigDTO conf =  tpcf.buildTaskProcessorConfig(scheduledTaskLockDTO);

        FileLoader fileLoader = new FileLoader();
        String dataSetKeyName = "transaction_dds";
        Map<String, Object> context = ObjectMapping.asMap(conf);//ObjectMapping.asMap(fileLoader.loadDataSetDTO(dataSetKeyName));
        String rendered = jinjava.render("{{ targetDataSetKeyName }}",context);
        String renderedMap = jinjava.render("{{ dataSets['transaction_dds'].fullTableName }}",context);
        String rendered2 = jinjava.render("",context);
        assert (dataSetKeyName.equals(rendered));

    }
}

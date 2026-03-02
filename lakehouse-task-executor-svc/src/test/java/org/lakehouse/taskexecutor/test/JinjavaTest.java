package org.lakehouse.taskexecutor.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class JinjavaTest {
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
    }

    JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();

    @Autowired
    ConfigRestClientApi configRestClientApi;
        @Test
    @Order(1)
    public void testJinjaAddDay() {
        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays('" + targetDateTime.format(dateTimeFormatter) + "', 10)}}";
        String renderedTemplate = jinJavaUtils.render(template, new HashMap<>());
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }


    @Test
    @Order(2)
    public void testJinjaAddDay2() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ adddays(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, Object> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinJavaUtils.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusDays(10).format(dateTimeFormatter)));
    }





    @Test
    @Order(3)
    public void testJinjaContextReplacement2() {

        String targetDateTimeStr = "2025-06-12T16:03:00.435821544+03:00";
        String template = "{{ " + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + " }}";
        Map<String, Object> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTimeStr));
        String renderedTemplate = jinJavaUtils.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTimeStr));
    }

    @Test
    @Order(2)
    public void testJinjaAddMonths() {

        OffsetDateTime targetDateTime = OffsetDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
        String template = "{{ addmonths(" + SystemVarKeys.TARGET_DATE_TIME_TZ_KEY + ",  10)}}";
        Map<String, Object> context = new HashMap<>(Map.of(SystemVarKeys.TARGET_DATE_TIME_TZ_KEY, targetDateTime.format(dateTimeFormatter)));
        String renderedTemplate = jinJavaUtils.render(template, context);
        System.out.println(renderedTemplate);
        assert (renderedTemplate.equals(targetDateTime.plusMonths(10).format(dateTimeFormatter)));
    }


    @Test
    @Order(6)
    public void testLoadDataSet() throws IOException {
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
        SourceConfDTO sourceConfDTO = configRestClientApiTest.getSourceConfDTO("transaction_dds");
        String dataSetKeyName = "transaction_dds";
        Map<String, Object> context = ObjectMapping.asMap(sourceConfDTO);//ObjectMapping.asMap(fileLoader.loadDataSetDTO(dataSetKeyName));
        String rendered = jinJavaUtils.render("{{ targetDataSetKeyName }}",context);
        String renderedMap = jinJavaUtils.render("{{ dataSets['transaction_dds'].fullTableName }}",context);
        String rendered2 = jinJavaUtils.render("",context);
        assert (dataSetKeyName.equals(rendered));

    }

    private SourceConfDTO getStubSourceConfDTO(){
        SourceConfDTO conf = new SourceConfDTO();
        DriverDTO stubDriverDTO = new DriverDTO();
        stubDriverDTO.setKeyName("stubDriver");
        DataSourceDTO stubDataSource = new DataSourceDTO();
        stubDataSource.setKeyName("stubDataSource");
        stubDataSource.setCatalogKeyName("StubCatalogName");
        stubDataSource.setDriverKeyName(stubDriverDTO.getKeyName());
        DataSetDTO dataSetDTO = new DataSetDTO();
        dataSetDTO.setDataSourceKeyName(stubDataSource.getKeyName());
        dataSetDTO.setTableName("testTable");
        dataSetDTO.setDatabaseSchemaName("testSchema");
        dataSetDTO.setKeyName("testDataSet");
        conf.setDataSets(Map.of(dataSetDTO.getKeyName(),dataSetDTO));
        conf.setDataSources(Map.of(stubDataSource.getKeyName(),stubDataSource));
        conf.setDrivers(Map.of(stubDriverDTO.getKeyName(),stubDriverDTO));
        conf.setTargetDataSetKeyName(dataSetDTO.getKeyName());
        return conf;
    }

    @Test
    @Order(5)
    public void testJinjavaRef() throws JsonProcessingException {
        SourceConfDTO conf = getStubSourceConfDTO();
        String template = "{{ ref('" + conf.getTargetDataSet().getKeyName() + "') }}";

        Map<String, Object> context = ObjectMapping.asMap(conf);
        String renderedTemplate = jinJavaUtils.render(template, context);
        System.out.println(renderedTemplate);

        String expected = conf.getTargetDataSet().getDatabaseSchemaName()+"."+conf.getTargetDataSet().getTableName();
        System.out.println(expected);

        assert (renderedTemplate.equals(expected));
    }
    @Test
    @Order(6)
    public void testEmptyDataSetKeyName(){
        boolean isOk = false;
        try {


            String template = "{{ refCat('') }}";
            jinJavaUtils.render(template, new HashMap<>());
        }catch (FatalTemplateErrorsException e){
            isOk = true;
        }

        assert (isOk);
    }
    @Test
    @Order(7)
    public void testJinjavaRefCat() throws JsonProcessingException {
        SourceConfDTO conf = getStubSourceConfDTO();

        String template = "{{ refCat('" + conf.getTargetDataSetKeyName() + "') }}";

        Map<String, Object> context = ObjectMapping.asMap(conf);
        String renderedTemplate = jinJavaUtils.render(template, context);
        System.out.println(renderedTemplate);

        String expected = conf.getTargetDataSource().getCatalogKeyName() + "."
                + conf.getTargetDataSet().getDatabaseSchemaName()+"."
                + conf.getTargetDataSet().getTableName();
        System.out.println(expected);

        assert (renderedTemplate.equals(expected));
    }


    @Test
    @Order(9)
    public void testRef() throws IOException {
        String dataSetKeyName = "transaction_dds";
        SourceConfDTO conf = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        Map<String, Object> context = ObjectMapping.asMap(conf);
        JinJavaUtils jinjava1 = JinJavaFactory.getJinJavaUtils();
        jinjava1.injectGlobalContext(context);
        String template =  "CREATE TABLE {{ ref(targetDataSetKeyName) }} (";
        String rendered = jinjava1.render(template, new HashMap<>());
        assert ("CREATE TABLE default.transaction_dds (".equals(rendered));

    }
    @Test
    @Order(10)
    public void testExtractColumnsDDL() throws IOException {
        String expected = "id bigint,\n" +
                "amount decimal,\n" +
                "client_id string,\n" +
                "client_name string,\n" +
                "commission string,\n" +
                "provider_id string,\n" +
                "reg_date_time timestamp";

        FileLoader fileLoader = new FileLoader();
        String dataSetKeyName = "transaction_dds";
        SourceConfDTO conf = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        conf.getDataSets().put(dataSetKeyName, fileLoader.loadDataSetDTO(dataSetKeyName));
        conf.setTargetDataSetKeyName(dataSetKeyName);
        Map<String, Object> context = ObjectMapping.asMap(conf);

        String template = "{%set targetDataSet=dataSets[targetDataSetKeyName]%}" +
                "{{ extractColumnsDDL(targetDataSet.columnSchema) }}";
        String rendered = jinJavaUtils.render(template,context);
        assert (expected.equals(rendered));

    }
    @Test
    @Order(12)
    void constraint() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DriverDTO driverDTO = fileLoader.loadDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DataSetDTO dataSetDTO = fileLoader.loadDataSetDTO("transaction_processing");
        SourceConfDTO conf = new SourceConfDTO();
        conf.getDataSources().put(dataSourceDTO.getKeyName(),dataSourceDTO);
        conf.getDataSets().put(dataSetDTO.getKeyName(),dataSetDTO);
        conf.setTargetDataSetKeyName(dataSetDTO.getKeyName());
        Map<String,Object> context = ObjectMapping.asMap(conf);

        String constraintName = dataSetDTO.getConstraints().entrySet().stream().filter(e -> e.getValue().getType().equals(Types.Constraint.primary)).map(Map.Entry::getKey).findFirst().get();
        context.put(SystemVarKeys.CONSTRAINT_NAME, constraintName);

        String template = driverDTO.getSqlTemplate().getPrimaryKeyDDL();
        String result = jinJavaUtils.render(template,context);
        String expected = "CONSTRAINT transaction_processing_pk PRIMARY KEY (id)";
        System.out.println("template: "+ template+"\nresult: " + result + "\nExpected: " + expected) ;
        assert (expected.equals(result));
    }
    @Test
    @Order(12)
    void constraintFK() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DriverDTO driverDTO = fileLoader.loadDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DataSetDTO targetDataSet = fileLoader.loadDataSetDTO("transaction_processing");
        DataSetDTO foreignDataSetDTO = fileLoader.loadDataSetDTO("client_processing");

        SourceConfDTO conf = new SourceConfDTO();
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        Map<String,Object> context = ObjectMapping.asMap(conf);

        Map.Entry<String, DataSetConstraintDTO> fkDTO = targetDataSet.getConstraints().entrySet().stream().filter(e -> e.getValue().getType().equals(Types.Constraint.foreign)).findFirst().orElseThrow();

        context.put("constraintName", fkDTO.getKey());

        String template = driverDTO.getSqlTemplate().getForeignKeyDDL();
        System.out.println("template is :" + template);
        String result = jinJavaUtils.render(template,context);
        System.out.println("result   is :" + result);
        String expected = "CONSTRAINT transaction_processing_client_fk FOREIGN KEY(client_id) REFERENCES proc.client(id)";
        System.out.println("expected is :" + expected);
        assert (expected.equals(result));

    }
    @Test
    @Order(13)
    void extractMergeUpdate() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DriverDTO driverDTO = fileLoader.loadDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DataSetDTO targetDataSet = fileLoader.loadDataSetDTO("transaction_processing");

        DataSetDTO foreignDataSetDTO = fileLoader.loadDataSetDTO("client_processing");

        SourceConfDTO conf = new SourceConfDTO();
        conf.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));

        Map<String,Object> context =ObjectMapping.asMap(conf);

        context.put("script", "select 1");

        Map<String, Object> s = ObjectMapping.asMap(driverDTO.getSqlTemplate());
        Map<String, Object>  sNonNull = s
                .entrySet()
                .stream()
                .filter(so-> so.getValue()!=null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String template =  "{%set targetDataSet=dataSets[targetDataSetKeyName]%}" +"{{ extractMergeUpdate(targetDataSet,'q') }}";
        System.out.println("template is :" + template);
        String result = jinJavaUtils.render(template,context);

        System.out.println("result   is :" + result);
        String expected = "\t\t\tamount = q.amount,\n" +
                "\t\t\tclient_id = q.client_id,\n" +
                "\t\t\tcommission = q.commission,\n" +
                "\t\t\tprovider_id = q.provider_id,\n" +
                "\t\t\treg_date_time = q.reg_date_time";
        System.out.println("expected is :" + expected);
        assert (expected.equals(result));

    }@Test
    @Order(13)
    void mergeDML() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DriverDTO driverDTO = fileLoader.loadDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DataSetDTO targetDataSet = fileLoader.loadDataSetDTO("transaction_processing");

        DataSetDTO foreignDataSetDTO = fileLoader.loadDataSetDTO("client_processing");

        SourceConfDTO conf = new SourceConfDTO();
        conf.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));

        Map<String,Object> context =ObjectMapping.asMap(conf);

        context.put("script", "select 1");

        Map<String, Object> s = ObjectMapping.asMap(driverDTO.getSqlTemplate());
        Map<String, Object>  sNonNull = s
                .entrySet()
                .stream()
                .filter(so-> so.getValue()!=null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        String template =   driverDTO.getSqlTemplate().getMergeDML();
        System.out.println("template is :" + template);
        String result = jinJavaUtils.render(template,context);

        System.out.println("result   is :" + result);
        String expected = "MERGE INTO  proc.transactions t\n" + "\tUSING (select 1) q\n" + "\tON (t.id = q.id)\n" + "\tWHEN MATCHED THEN\n" + "\t\tUPDATE SET \t\t\tamount = q.amount,\n" + "\t\t\tclient_id = q.client_id,\n" + "\t\t\tcommission = q.commission,\n" + "\t\t\tprovider_id = q.provider_id,\n" + "\t\t\treg_date_time = q.reg_date_time\n" + "\tWHEN NOT MATCHED THEN\n" + "\t\tINSERT(id,amount,client_id,commission,provider_id,reg_date_time)\n" + "\t\tVALUES (q.id,q.amount,q.client_id,q.commission,q.provider_id,q.reg_date_time)";
        System.out.println("expected is :" + expected);
        assert (expected.equals(result));

    }

    @Test
    @Order(14)
    void taskProcessorArg() throws IOException {
        ScheduledTaskLockDTO conf = new ScheduledTaskLockDTO();
        String argName =  "protocol";
        String expected = "https";
        conf.setScheduledTaskEffectiveDTO(new ScheduledTaskDTO());
        conf.getScheduledTaskEffectiveDTO().getTaskProcessorArgs().put(argName,expected);
        String template = "{{scheduledTaskEffectiveDTO.taskProcessorArgs['" + argName + "']}}";

        Map<String,Object> context =ObjectMapping.asMap(conf);

        String result = jinJavaUtils.render(template,context);

        assert (expected.equals(result));
    }
    @Test
    void dataSourcePropsRender() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DriverDTO driverDTO = fileLoader.loadDriverDTO(dataSourceDTO.getDriverKeyName());

        Map<String,Object> localContext = new HashMap<>();
        localContext.put(SystemVarKeys.DRIVER_KEY, driverDTO);
        localContext.put(SystemVarKeys.SERVICE_KEY,dataSourceDTO.getService());
        String expected = "jdbc:postgresql://localhost:5432/postgresDB";
        String template = "{{driver.connectionTemplates['jdbc']}}";//dataSourceDTO.getServices().get(0).getProperties().get("spark.sql.catalog.processingdb.url");
        String result = jinJavaUtils.render(template,localContext);
        assert expected.equals(result);
    }

    @Test
    void sparkRestUrlRender() throws JsonProcessingException {

        String dataSetKeyName = "transaction_dds";
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        String template = sourceConfDTO.getTargetDriver().getConnectionTemplates().get(Types.ConnectionType.spark);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.getTaskProcessorArgs().put(SystemVarKeys.CONNECTION_STRING_PROTOCOL_NAME,"http");
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
        String url = jinJavaUtils.render(template);
        System.out.println(url);
        System.out.println(template);
        assert ("http://localhost:6066".equals(url));

    }

    @Test
    void catalogDataBaseSchema() throws JsonProcessingException {

        String dataSetKeyName = "transaction_dds";
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(dataSetKeyName);

        Map<String,Object> localContext = ObjectMapping.asMap(sourceConfDTO);

        String template = "{{refCatSchema(" + SystemVarKeys.TARGET_DATASET_KEY_NAME + ")}}";
        String expected = "lakehouse.default";
        String result = jinJavaUtils.render(template,localContext);

        assert(expected.equals(result));
    }
}

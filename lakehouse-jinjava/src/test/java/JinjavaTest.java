import com.fasterxml.jackson.core.JsonProcessingException;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.FatalTemplateErrorsException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"}, classes = {JinJavaConfiguration.class})
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
    @Order(4)
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
    @Order(5)
    public void testJinjavaRef() throws JsonProcessingException {

        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        DataSetDTO dataSetDTO = new DataSetDTO();
        dataSetDTO.setTableName("testTable");
        dataSetDTO.setDatabaseSchemaName("testSchema");
        dataSetDTO.setKeyName("testDataSet");
        conf.setDataSets(Map.of(dataSetDTO.getKeyName(),dataSetDTO));

        String template = "{{ ref('" + dataSetDTO.getKeyName() + "') }}";

        Map<String, Object> context = ObjectMapping.asMap(conf);
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);

        String expected = dataSetDTO.getDatabaseSchemaName()+"."+dataSetDTO.getTableName();
        System.out.println(expected);

        assert (renderedTemplate.equals(expected));
    }
    @Test
    @Order(6)
    public void testEmptyDataSetKeyName(){
        boolean isOk = false;
        try {


            String template = "{{ refCat('') }}";
            jinjava.render(template, new HashMap<>());
        }catch (FatalTemplateErrorsException e){
            isOk = true;
        }

                assert (isOk);
    }
    @Test
    @Order(7)
    public void testJinjavaRefCat() throws JsonProcessingException {

        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        DataSetDTO dataSetDTO = new DataSetDTO();
        dataSetDTO.setTableName("testTable");
        dataSetDTO.setDatabaseSchemaName("testSchema");
        dataSetDTO.setKeyName("testDataSet");
        dataSetDTO.setDataSourceKeyName("testSource");
        conf.setDataSets(Map.of(dataSetDTO.getKeyName(),dataSetDTO));

        String template = "{{ refCat('" + dataSetDTO.getKeyName() + "') }}";

        Map<String, Object> context = ObjectMapping.asMap(conf);
        String renderedTemplate = jinjava.render(template, context);
        System.out.println(renderedTemplate);

        String expected = dataSetDTO.getDataSourceKeyName() + "." + dataSetDTO.getDatabaseSchemaName()+"."+dataSetDTO.getTableName();
        System.out.println(expected);

        assert (renderedTemplate.equals(expected));
    }


    @Test
    @Order(9)
    public void testRef() throws IOException {
        FileLoader fileLoader = new FileLoader();
        String dataSetKeyName = "transaction_dds";
        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.setTargetDataSetKeyName(dataSetKeyName);
        DataSetDTO  dataSetDTO = fileLoader.loadDataSetDTO(dataSetKeyName);
        conf.getDataSets().put(dataSetKeyName,dataSetDTO);
        Map<String, Object> context = ObjectMapping.asMap(conf);
        Jinjava jinjava1 = JinJavaFactory.getJinjava();
        jinjava1.getGlobalContext().putAll(context);
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
        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.getDataSets().put(dataSetKeyName, fileLoader.loadDataSetDTO(dataSetKeyName));
        conf.setTargetDataSetKeyName(dataSetKeyName);
        Map<String, Object> context = ObjectMapping.asMap(conf);

        String template = "{%set targetDataSet=dataSets[targetDataSetKeyName]%}" +
                "{{ extractColumnsDDL(targetDataSet.columnSchema) }}";
        String rendered = jinjava.render(template,context);
        assert (expected.equals(rendered));

    }
    @Test
    @Order(12)
    void constraint() throws IOException {
        FileLoader fileLoader = new FileLoader();
        DriverDTO driverDTO = fileLoader.loadDriverDTO("postgres");
        DataSourceDTO dataSourceDTO = fileLoader.loadDataSourceDTO("processingdb");
        DataSetDTO dataSetDTO = fileLoader.loadDataSetDTO("transaction_processing");
        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.getDataSources().put(dataSourceDTO.getKeyName(),dataSourceDTO);
        conf.getDataSets().put(dataSetDTO.getKeyName(),dataSetDTO);
        conf.setTargetDataSetKeyName(dataSetDTO.getKeyName());
        Map<String,Object> context = ObjectMapping.asMap(conf);

        String constraintName = dataSetDTO.getConstraints().entrySet().stream().filter(e -> e.getValue().getType().equals(Types.Constraint.primary)).map(Map.Entry::getKey).findFirst().get();
        context.put(SystemVarKeys.CONSTRAINT_NAME, constraintName);

        String template = driverDTO.getSqlTemplate().getPrimaryKeyDDL();
        String result = jinjava.render(template,context);
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

        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        Map<String,Object> context = ObjectMapping.asMap(conf);

        Map.Entry<String,DataSetConstraintDTO> fkDTO = targetDataSet.getConstraints().entrySet().stream().filter(e -> e.getValue().getType().equals(Types.Constraint.foreign)).findFirst().orElseThrow();

        context.put("constraintName", fkDTO.getKey());

        String template = driverDTO.getSqlTemplate().getForeignKeyDDL();
        System.out.println("template is :" + template);
        String result = jinjava.render(template,context);
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

        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));
        conf.setScripts(Map.of(
                targetDataSet.getScripts().get(0).getKey(),
                fileLoader.loadModelScript(targetDataSet.getKeyName())
        ));

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
        String result = jinjava.render(template,context);

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

        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        conf.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        conf.setTargetDataSetKeyName(targetDataSet.getKeyName());
        conf.setDataSources(Map.of(dataSourceDTO.getKeyName(),dataSourceDTO));
        conf.setDataSets(Map.of(
                targetDataSet.getKeyName(),targetDataSet,
                foreignDataSetDTO.getKeyName(),foreignDataSetDTO));
        conf.setScripts(Map.of(
                targetDataSet.getScripts().get(0).getKey(),
                fileLoader.loadModelScript(targetDataSet.getKeyName())
        ));

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
        String result = jinjava.render(template,context);

        System.out.println("result   is :" + result);
        String expected = "MERGE INTO  proc.transactions t\n" + "\tUSING (select 1) q\n" + "\tON (t.id = q.id)\n" + "\tWHEN MATCHED THEN\n" + "\t\tUPDATE SET \t\t\tamount = q.amount,\n" + "\t\t\tclient_id = q.client_id,\n" + "\t\t\tcommission = q.commission,\n" + "\t\t\tprovider_id = q.provider_id,\n" + "\t\t\treg_date_time = q.reg_date_time\n" + "\tWHEN NOT MATCHED THEN\n" + "\t\tINSERT(id,amount,client_id,commission,provider_id,reg_date_time)\n" + "\t\tVALUES (q.id,q.amount,q.client_id,q.commission,q.provider_id,q.reg_date_time)";
        System.out.println("expected is :" + expected);
        assert (expected.equals(result));

    }

    @Test
    @Order(14)
    void taskProcessorArg() throws IOException {
        TaskProcessorConfigDTO conf = new TaskProcessorConfigDTO();
        String argName =  "protocol";
        String expected = "https";
        conf.getTaskProcessorArgs().put(argName,expected);
        String template = "{{taskProcessorArgs['" + argName + "']}}";

        Map<String,Object> context =ObjectMapping.asMap(conf);

        String result = jinjava.render(template,context);

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
        String result = jinjava.render(template,localContext);
        assert expected.equals(result);
    }
}

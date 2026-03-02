package org.lakehouse.taskexecutor;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.utils.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.BodyParamImpl;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.lakehouse.taskexecutor.api.processor.body.sql.MergeSQLProcessorBody;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Import({MergeSQLProcessorBody.class})
public class SparkTaskProcessorBodyTest {
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        JinJavaUtils getJinJavaUtils(){
            return new JinJavaConfiguration().getJinJavaUtils();
        }
    }
    @Autowired
    JinJavaUtils jinJavaUtils;
    @Autowired
    ConfigurableApplicationContext applicationContext;

    String clientDatasetName = "client_processing";
    String trnDatasetName = "transaction_processing";
    String trnddsDatasetName = "transaction_dds";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );
    ConfigRestClientApi configRestClientApi = new ConfigRestClientApiTest();
    public SparkTaskProcessorBodyTest() throws IOException {
    }

    @BeforeAll
    static void beforeAllStart() {
        kafka.start();
        postgres.start();
    }

    @AfterAll
    static void afterAllDown(){
        kafka.stop();
        postgres.stop();
    }

    public SparkSession buildSparkSession(ScheduledTaskDTO t, SourceConfDTO sourceConfDTO){
        Map<String, Object> conf = new HashMap<>();
        sourceConfDTO.getDataSources().forEach((s, dataSourceDTO) -> {
            conf.putAll(SparkConfUtil.startWithSpark(dataSourceDTO.getService().getProperties()));
        });
        conf.putAll(SparkConfUtil.extractAppConf(t.getTaskProcessorArgs()));
        return SparkSession.builder().master("local").config(conf).getOrCreate();
    }

    private Dataset<Row> getClientDataSet(SparkSession sparkSession){
        return sparkSession.sql("select 1 as id,'Client Name' as name, TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "' as reg_date_time\n" +
                "union all\n" +
                "select 2,'Client2 Name2', TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "' \n");

    }
    private Dataset<Row> getTrnDataSet(SparkSession sparkSession){
        return sparkSession.sql("select 1 id ,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "' reg_date_time, 1 client_id, 1 provider_id, 10.2 amount, 0.5 commission \n" +
                "union all\n" +
                "select 2,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "', 1, 1,11.2,0.4 \n" +
                "union all\n" +
                "select 3,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "', 2, 1,13.9,0.8 \n"
        );

    }
/*    private void dsmRead(DataSourceManipulator dsm) throws ExecuteException {
        dsm.executeUtils().executeQuery("select * from {{refCat(targetDataSetKeyName)}}").show(false);

    }*/
    private DataSourceManipulator createPgDSM(
            SparkSession sparkSession,
            String dataSetKeyName,
            Dataset<Row> initData) throws CreateException, IOException, UnsuportedDataSourceException, NoSuchTableException {

        SourceConfDTO conf = configRestClientApi.getSourceConfDTO(dataSetKeyName); // new TaskConfigTestFactory().loadTaskProcessorConfigDTO(dataSetKeyName, "prepare");
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(conf));
        DataSourceManipulator jdbcManipulator = DataSourceManipulatorFactory
                .buildDataSourceManipulator(
                        conf.getDriverDTOByDataSetKeyName(dataSetKeyName),
                        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName),
                        conf.getDataSets().get(dataSetKeyName),jinJavaUtils);

        DataSourceManipulator pgDSM = DataManipulators.getSparkSQLDataSourceManipulatorPg(
                jinJavaUtils,postgres,sparkSession,dataSetKeyName,conf);

        //create in postgres
        jdbcManipulator.createTableIfNotExists();
        // create in spark catalog
        pgDSM.createTableIfNotExists();
        String catTabName = jinJavaUtils.render("{{refCat('" + dataSetKeyName + "')}}");
        initData.show();
        initData.writeTo(catTabName).append();
        sparkSession.sql("select * from " + catTabName).show();
        //dsmRead(pgDSM);

        //pgDSM.write(initData,  Configuration.ModificationRule.append);
        //dsmRead(pgDSM);
        return pgDSM;
    }

    private void dropPgDsm(DataSourceManipulator dsm, String dataSetKeyName) throws IOException, DropException {
        SourceConfDTO conf = configRestClientApi.getSourceConfDTO(dataSetKeyName);
                //new TaskConfigTestFactory().loadTaskProcessorConfigDTO(dataSetKeyName, "prepare");
       // Jinjava jinjava = JinJavaFactory.getJinJavaUtils(conf);
        DataSourceDTO dataSourceDTO = conf.getTargetDataSource();
        dataSourceDTO.getService().setHost(postgres.getHost());
        dataSourceDTO.getService().setPort(postgres.getMappedPort(5432).toString());
        dataSourceDTO.getService().setUrn(postgres.getDatabaseName());
        dataSourceDTO.getService().getProperties().put("user", postgres.getUsername());
        dataSourceDTO.getService().getProperties().put("password", postgres.getPassword());
        DriverDTO driverDTO = conf.getTargetDriver();
        DataSetDTO dataSetDTO = conf.getTargetDataSet();

        DataSourceManipulator jdbcManipulator = DataSourceManipulatorFactory.buildDataSourceManipulator(
                driverDTO,dataSourceDTO,dataSetDTO, jinJavaUtils);
        jdbcManipulator.drop();
        dsm.drop();
    }
    @Test
    @Order(1)
    void testPostgres() throws IOException, ReadException, DropException, UnsuportedDataSourceException, WriteException, DDLDIalectException, CreateException, ExecuteException, NoSuchTableException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskLockDTO scheduledTaskLockDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load");
        //TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load");
        SparkSession sparkSession = buildSparkSession(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), sourceConfDTO);

       JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
       jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));

        DataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));

        long rows =  sparkSession.sql(
                jinJavaUtils.render(
                "select * from {{refCat('" + clientDatasetName + "')}}" )).count(); //clientDSM.read(new HashMap<>()).count();

        dropPgDsm(clientDSM,clientDatasetName);
        sparkSession.stop();
        assert (rows == 2);
    }



    @Test
    void testIcebergTable() throws TaskFailedException, IOException, CreateException, ReadException, WriteException, DropException, DDLDIalectException, UnsuportedDataSourceException, ExecuteException, TaskConfigurationException, NoSuchTableException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskLockDTO scheduledTaskLockDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load");
        SparkSession sparkSession = buildSparkSession(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), sourceConfDTO);
        jinJavaUtils.cleanGlobalContext();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO()));

        DataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));// DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,clientDatasetName,conf);
        DataSourceManipulator trnDSM = createPgDSM(sparkSession,trnDatasetName,getTrnDataSet(sparkSession)); // DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,trnDatasetName,conf);
        DataSourceManipulator trnddsDSM = DataManipulators.getIcebergDataSourceManipulator(jinJavaUtils,sparkSession, trnddsDatasetName,sourceConfDTO);

        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + clientDatasetName + "')}}" )).show();
        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnDatasetName + "')}}" )).show();
        trnddsDSM.createTableIfNotExists();
        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnddsDatasetName + "')}}" )).show();
        BodyParam bodyParam = new BodyParamImpl(
                trnddsDSM,
                Map.of(clientDatasetName, clientDSM, trnDatasetName,trnDSM),
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessorArgs());

        ProcessorBody pb = applicationContext.getBean(MergeSQLProcessorBody.class);
        pb.run(bodyParam);
        long rows = sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnddsDatasetName + "')}}" )).count();//trnddsDSM.read(new HashMap<>()).count();
        dropPgDsm(trnDSM,trnDatasetName);
        dropPgDsm(clientDSM,clientDatasetName);
        trnddsDSM.drop();
        assert (rows==3);
    }

    @Test
    @Order(4)
    void testDropTableIceberg() throws  IOException, CreateException, DropException, UnsuportedDataSourceException {

        String trnddsDatasetName = "transaction_dds";
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskLockDTO scheduledTaskLockDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load");
        SparkSession sparkSession = buildSparkSession(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), sourceConfDTO);
        jinJavaUtils.cleanGlobalContext();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));


        DataSourceManipulator dsm = DataManipulators.getIcebergDataSourceManipulator(jinJavaUtils,sparkSession,trnddsDatasetName,sourceConfDTO);

        String catTableName =
                sourceConfDTO.getTargetDataSource().getCatalogKeyName() +"." +
                        sourceConfDTO.getTargetDataSet().getDatabaseSchemaName() +"." +
                        sourceConfDTO.getTargetDataSet().getTableName();
        boolean isExists = sparkSession.catalog().tableExists(catTableName);
        if ( isExists )
            throw new CreateException("Table found, but not expected");

        dsm.createTableIfNotExists();
        isExists = sparkSession.catalog().tableExists( catTableName);
        if ( !isExists )
            throw new CreateException("Table not found");
        dsm.drop();
        isExists = sparkSession.catalog().tableExists( catTableName);
        sparkSession.stop();
        assert  ( !isExists);

    }
}

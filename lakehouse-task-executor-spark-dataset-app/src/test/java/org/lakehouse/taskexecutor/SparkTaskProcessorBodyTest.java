package org.lakehouse.taskexecutor;

import org.apache.spark.SparkConf;
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
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactoryImpl;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.sql.MergeSQLProcessorBody;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.UnsuportedDataSourceException;
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
import java.util.List;
import java.util.Map;

@SpringBootTest
@Import({
        MergeSQLProcessorBody.class})
public class SparkTaskProcessorBodyTest {

    static String clientDatasetName = "client_processing";
    static String trnDatasetName = "transaction_processing";
    static String trnddsDatasetName = "transaction_dds";
    @Configuration
    static class ContextConfiguration {
        @Bean
        @Primary
        ConfigRestClientApi getConfigRestClientApi() throws IOException {
            return new ConfigRestClientApiTest(); //stub
        }
        @Bean
        @Primary
        SparkSession getSparkSessionTest() throws IOException {
            ConfigRestClientApi configRestClientApi1 =  new ConfigRestClientApiTest();
            DataSetDTO dataSetDTO = configRestClientApi1.getDataSetDTO(trnddsDatasetName);
            DataSourceDTO dataSourceDTO = configRestClientApi1.getDataSourceDTO(dataSetDTO.getDataSourceKeyName());
            SparkConf conf = new SparkConf();
            SparkConfUtil.extractAppConf(dataSourceDTO.getService().getProperties()).forEach(conf::set);
            return SparkSession.builder().master("local").config(conf).getOrCreate();
        }
        @Bean
        DataSourceManipulatorFactory getDataSourceManipulatorFactory(SparkSession sparkSession){
            return new SparkDataSourceManipulatorFactory(sparkSession);
        }
    }
    @Autowired
    ConfigurableApplicationContext applicationContext;

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
    private DataSourceManipulator createPgDSM(
            SparkSession sparkSession,
            String dataSetKeyName,
            Dataset<Row> initData) throws CreateException, IOException, NoSuchTableException, TaskConfigurationException {

        SourceConfDTO conf = configRestClientApi.getSourceConfDTO(dataSetKeyName);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(conf));
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().setHost(postgres.getHost());
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().setPort(postgres.getMappedPort(5432).toString());
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().setHost(postgres.getHost());
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().setUrn(postgres.getDatabaseName());
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().getProperties().put("user", postgres.getUsername());
        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().getProperties().put("password", postgres.getPassword());
        // pg dynamic test properties
        Map.of(
                "spark.sql.catalog.processing", "org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog",
                "spark.sql.catalog.processing.url", postgres.getJdbcUrl(),
                "spark.sql.catalog.processing.user", postgres.getUsername(),
                "spark.sql.catalog.processing.password", postgres.getPassword())
                .forEach((k,v) ->
                        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().getProperties().put(k, v));

        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getService().getProperties()
                .forEach((k, v) -> sparkSession.conf().set(k,v));

        new CatalogActivatorService(sparkSession).activate(List.of(conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName)));

        DataSourceManipulator jdbcManipulator = new DataSourceManipulatorFactoryImpl()
                .buildDataSourceManipulator(
                        conf.getDriverDTOByDataSetKeyName(dataSetKeyName),
                        conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName),
                        conf.getDataSets().get(dataSetKeyName),jinJavaUtils,configRestClientApi);

        //create in postgres
        jdbcManipulator
                .createTableIfNotExists();

        sparkSession.catalog().listCatalogs().show();
        sparkSession.catalog().setCurrentCatalog(conf.getDataSourceDTOByDataSetKeyName(dataSetKeyName).getCatalogKeyName());
        sparkSession.catalog().listDatabases().show();

        String catTabName = jinJavaUtils.render("{{refCat('" + dataSetKeyName + "')}}");
        initData.show();
        initData.writeTo(catTabName).append();
        sparkSession.sql("select * from " + catTabName).show();
        return jdbcManipulator;//pgDSM;
    }

    @Test
    @Order(1)
    void testPostgres() throws IOException, DropException,  CreateException, NoSuchTableException, TaskConfigurationException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskLockDTO scheduledTaskLockDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load");
        SparkSession sparkSession =  applicationContext.getBean(SparkSession.class);/*buildSparkSession(
                applicationContext.getBean(SparkSession.class),
                scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), sourceConfDTO);

  */
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));

        DataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));

        long rows =  sparkSession.sql(
                jinJavaUtils.render(
                "select * from {{refCat('" + clientDatasetName + "')}}" )).count(); //clientDSM.read(new HashMap<>()).count();

        clientDSM.drop();

        assert (rows == 2);
    }



    @Test
    void testIcebergTable() throws TaskFailedException, IOException, CreateException, ReadException, WriteException, DropException, DDLDIalectException, UnsuportedDataSourceException, ExecuteException, TaskConfigurationException, NoSuchTableException {
        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskDTO scheduledTaskDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load").getScheduledTaskEffectiveDTO();
        SparkSession sparkSession = applicationContext.getBean(SparkSession.class);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

        DataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));// DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,clientDatasetName,conf);
        DataSourceManipulator trnDSM = createPgDSM(sparkSession,trnDatasetName,getTrnDataSet(sparkSession)); // DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,trnDatasetName,conf);
        DataSourceManipulator trnddsDSM = DataManipulators
                .getIcebergDataSourceManipulator(jinJavaUtils,sparkSession, trnddsDatasetName,sourceConfDTO,configRestClientApi);

        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + clientDatasetName + "')}}" )).show();
        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnDatasetName + "')}}" )).show();
        trnddsDSM.createTableIfNotExists();
        sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnddsDatasetName + "')}}" )).show();

        ProcessorBody pb = applicationContext.getBean(MergeSQLProcessorBody.class);
        pb.run(scheduledTaskDTO);
        Dataset<Row> resultDf = sparkSession.sql(jinJavaUtils.render("select * from {{refCat('" + trnddsDatasetName + "')}}" ));
        resultDf.show();
        long rows = resultDf.count();
        trnDSM.drop();
        clientDSM.drop();
        trnddsDSM.drop();
        assert (rows==3);
    }

    @Test
    @Order(4)
    void testDropTableIceberg() throws CreateException, DropException, TaskConfigurationException, IOException {

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(trnddsDatasetName);
        ScheduledTaskDTO scheduledTaskDTO = new  TaskConfigTestFactory().loadScheduledTaskLockDTO(trnddsDatasetName,"load").getScheduledTaskEffectiveDTO();
        SparkSession sparkSession = applicationContext.getBean(SparkSession.class);
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
        jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));


        DataSourceManipulator dsm = DataManipulators
                .getIcebergDataSourceManipulator(jinJavaUtils,sparkSession,trnddsDatasetName,sourceConfDTO,configRestClientApi);

        String catTableName =
                sourceConfDTO.getTargetDataSource().getCatalogKeyName() +"." +
                        sourceConfDTO.getTargetDataSet().getDatabaseSchemaName() +"." +
                        sourceConfDTO.getTargetDataSet().getTableName();
        dsm.createTableIfNotExists();
        boolean isExists = sparkSession.catalog().tableExists(catTableName);
        if ( !isExists )
            throw new CreateException("Table not found");
        dsm.drop();
        isExists = sparkSession.catalog().tableExists(catTableName);

        assert  (!isExists);
    }
}

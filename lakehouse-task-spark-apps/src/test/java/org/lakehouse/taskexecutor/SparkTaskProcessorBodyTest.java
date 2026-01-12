package org.lakehouse.taskexecutor;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskConfigBuildException;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBodyFactory;
import org.lakehouse.taskexecutor.api.processor.body.SparkProcessorBodyParamFactory;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class SparkTaskProcessorBodyTest {
    String clientDatasetName = "client_processing";
    String trnDatasetName = "transaction_processing";
    String trnddsDatasetName = "transaction_dds";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    public SparkTaskProcessorBodyTest() {
    }

    @BeforeAll
    static void beforeAllStart() {
        postgres.start();
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
    private void dsmRead(SparkSQLDataSourceManipulator dsm) throws ExecuteException {
        dsm.executeUtils().executeQuery("select * from {{refCat(targetDataSetKeyName)}}").show(false);

    }
    private SparkSQLDataSourceManipulator createPgDSM(
            SparkSession sparkSession,
            String dataSetKeyName,
            Dataset<Row> initData) throws CreateException, ReadException, WriteException, TaskConfigBuildException, IOException, UnsuportedDataSourceException, ExecuteException {

        TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(dataSetKeyName, "prepare");
        Jinjava jinjava = JinJavaFactory.getJinjava(conf);
        DataSourceManipulator jdbcManipulator = DataSourceManipulatorFactory.buildDataSourceManipulator(conf,jinjava);

        SparkSQLDataSourceManipulator pgDSM = DataManipulators.getSparkSQLDataSourceManipulatorPg(
                jinjava,postgres,sparkSession,dataSetKeyName,conf);

        //create in postgres
        jdbcManipulator.createTableIfNotExists();
        // create in spark catalog
        pgDSM.createTableIfNotExists();
        dsmRead(pgDSM);
        pgDSM.write(initData,  Configuration.ModificationRule.append);
        dsmRead(pgDSM);
        return pgDSM;
    }

    private void dropPgDsm(DataSourceManipulator dsm, String dataSetKeyName) throws IOException, TaskConfigBuildException, DropException {
        TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(dataSetKeyName, "prepare");
        Jinjava jinjava = JinJavaFactory.getJinjava(conf);
        DataSourceDTO dataSourceDTO = conf.getTargetDataSourceDTO();
        dataSourceDTO.getServices().get(0).setHost(postgres.getHost());
        dataSourceDTO.getServices().get(0).setPort(postgres.getMappedPort(5432).toString());
        dataSourceDTO.getServices().get(0).setUrn(postgres.getDatabaseName());
        dataSourceDTO.getServices().get(0).getProperties().put("user", postgres.getUsername());
        dataSourceDTO.getServices().get(0).getProperties().put("password", postgres.getPassword());

        DataSourceManipulator jdbcManipulator = DataSourceManipulatorFactory.buildDataSourceManipulator(conf,jinjava);
        jdbcManipulator.drop();
        dsm.drop();
    }
    @Test
    @Order(1)
    void testPostgres() throws IOException, ReadException, DropException, TaskConfigBuildException, UnsuportedDataSourceException, WriteException, DDLDIalectException, CreateException, ExecuteException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(trnddsDatasetName,"load");

        Jinjava jinjava = JinJavaFactory.getJinjava(conf);

        SparkSQLDataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));

        long rows = clientDSM.read(new HashMap<>()).count();

        dropPgDsm(clientDSM,clientDatasetName);
        sparkSession.stop();
        assert (rows == 2);
    }


    /*@Test
    @Order(2)
    void
    testPostgresViolateConstraint() throws UnsuportedDataSourceException, IOException, CreateException, ReadException, DDLDIalectException, DropException, TaskConfigBuildException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        SparkSQLDataSourceManipulator clientDSM = null;


        try {
            clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession) );;
        }catch (WriteException e){
            e.printStackTrace();
        }
        //wrong write expect fail by pk
        boolean writeError = false;
        try {
            clientDSM.write(getClientDataSet(sparkSession), Configuration.ModificationRule.append);
        }catch (WriteException e){
            writeError = true;
        }

        clientDSM.read(new HashMap<>()).show();
        clientDSM.drop();
        sparkSession.stop();
        assert (writeError);
    }
*/
  /*  @Test
    @Order(3)
    void
    testPostgresFk() throws IOException, CreateException, WriteException, ReadException, DropException, DDLDIalectException, TaskConfigBuildException, UnsuportedDataSourceException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        SparkSQLDataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession) );;
        SparkSQLDataSourceManipulator trnDSM = createPgDSM(sparkSession,trnDatasetName,getTrnDataSet(sparkSession) );;


        long rows = clientDSM.read(new HashMap<>()).count();
        boolean dropErr = false;
        try {
            clientDSM.drop();
            trnDSM.drop();
        } catch (DropException e) {
            dropErr = true;
            trnDSM.drop();
            clientDSM.drop();
        }
        sparkSession.stop();
        assert (rows == 2);
        assert (dropErr);
    }

*/

    @Test
    void testIcebergTable() throws TaskFailedException, IOException, CreateException, ReadException, WriteException, TaskConfigBuildException, DropException, DDLDIalectException, UnsuportedDataSourceException, ExecuteException, TaskConfigurationException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();



        TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(trnddsDatasetName,"load");
        Jinjava jinjava = JinJavaFactory.getJinjava(conf);

        SparkSQLDataSourceManipulator clientDSM = createPgDSM(sparkSession,clientDatasetName,getClientDataSet(sparkSession));// DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,clientDatasetName,conf);
        SparkSQLDataSourceManipulator trnDSM = createPgDSM(sparkSession,trnDatasetName,getTrnDataSet(sparkSession)); // DataManipulators.getSparkSQLDataSourceManipulatorPg(jinjava,postgres,sparkSession,trnDatasetName,conf);
        SparkSQLDataSourceManipulator trnddsDSM = DataManipulators.getIcebergDataSourceManipulator(jinjava,sparkSession, trnddsDatasetName,conf);
        clientDSM.read(new HashMap<>()).show();
        trnDSM.read(new HashMap<>()).show();
        trnddsDSM.createTableIfNotExists();
        trnddsDSM.read(new HashMap<>()).show();

        ProcessorBody tb =  ProcessorBodyFactory.build(
                SparkProcessorBodyParamFactory
                        .buildSparkProcessorBodyParameter(
                                sparkSession,
                                conf),
                conf.getTaskProcessorBody());
        tb.run();
        trnddsDSM.read(new HashMap<>()).show();
        long rows = trnddsDSM.read(new HashMap<>()).count();
        dropPgDsm(trnDSM,trnDatasetName);
        dropPgDsm(clientDSM,clientDatasetName);
        trnddsDSM.drop();
        assert (rows==3);
    }

    @Test
    @Order(4)
    void testDropTableIceberg() throws DDLDIalectException, TaskConfigBuildException, IOException, CreateException, DropException, UnsuportedDataSourceException {
        SparkSession sparkSession = SparkSession.builder().master("local[*]").getOrCreate();
        String trnddsDatasetName = "transaction_dds";
        TaskProcessorConfigDTO conf = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(trnddsDatasetName,"load");
        Jinjava jinjava = JinJavaFactory.getJinjava(conf);

        SparkSQLDataSourceManipulator dsm = DataManipulators.getIcebergDataSourceManipulator(jinjava,sparkSession,trnddsDatasetName,conf);

        String catTableName =
                conf.getTargetDataSet().getDataSourceKeyName() +"." +
                        conf.getTargetDataSet().getDatabaseSchemaName() +"." +
                        conf.getTargetDataSet().getTableName();
        boolean isExists = sparkSession.catalog().tableExists(catTableName);
        dsm.createTableIfNotExists();
        isExists = sparkSession.catalog().tableExists( catTableName);
        if ( !isExists )
            throw new CreateException("Table not found");
        dsm.drop();
        isExists = sparkSession.catalog().tableExists( catTableName);
        sparkSession.stop();
        assert  ( !isExists);

    }
    private void executeJdbcQuery(String sql) throws SQLException {
        String url = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String password = postgres.getPassword();
        Connection connection = DriverManager.getConnection(url, user, password);
        Statement statement = connection.createStatement();
        statement.execute(sql);
    }

}

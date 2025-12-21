package org.lakehouse.taskexecutor;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Configuration;


import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.factory.TaskConfigBuildException;
import org.lakehouse.taskexecutor.executionmodule.body.BodyParamImpl;
import org.lakehouse.taskexecutor.executionmodule.body.TransformationSparkProcessorBody;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;
import org.lakehouse.taskexecutor.executionmodule.body.transformer.TransformationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SparkTaskProcessorBodyTest {
     DataManipulators dataManipulators = new DataManipulators();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("test")
            .withUsername("name").withPassword("password");

    public SparkTaskProcessorBodyTest() {
    }

    @BeforeAll
    static void beforeAllStart() {
        postgres.start();
    }

    private ServiceDTO parceUrlToServiceDTO(String url, String user, String password) {
        //"jdbc:postgresql://localhost:5432/mydb?sslmode=disable";
        String[] arr = url.replaceAll("jdbc:postgresql://", "").split("/");
        ServiceDTO result = new ServiceDTO();
        result.setUrn(arr[1]);
        result.setHost(arr[0].split(":")[0]);
        result.setPort(arr[0].split(":")[1]);
        result.setProperties(Map.of("user", user, "password", password));
        return result;
    }


    private Dataset<Row> getClientDataSet(SparkSession sparkSession){
        return sparkSession.sql("select 1 as id,'Client Name' as name, current_timestamp as reg_date_time\n" +
                "union all\n" +
                "select 2,'Client2 Name2', current_timestamp \n");

    }
    private Dataset<Row> getTrnDataSet(SparkSession sparkSession){
        return sparkSession.sql("select 1 id ,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "' reg_date_time, 1 client_id, 1 provider_id, 10.2 amount, 0.5 commission \n" +
                "union all\n" +
                "select 2,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "', 1, 1,11.2,0.4 \n" +
                "union all\n" +
                "select 3,TIMESTAMP '"+ TaskConfigTestFactory.intervalStart + "', 2, 1,13.9,0.8 \n"
        );

    }

    private DataSourceManipulator createClientDSM(SparkSession sparkSession) throws CreateException, ReadException, WriteException, TaskFailedException, TaskConfigBuildException, IOException {
        DataSourceManipulator clientDSM = dataManipulators.getPgDataSourceManipulator("client_processing",sparkSession, postgres);
        clientDSM.createIfNotExists();
        clientDSM.read(new HashMap<>()).show();
        clientDSM.write(getClientDataSet(sparkSession), new HashMap<>(), Configuration.ModificationRule.append);
        clientDSM.read(new HashMap<>()).show();
        return clientDSM;
    }

    private DataSourceManipulator createTrnDSM(SparkSession sparkSession) throws TaskFailedException, TaskConfigBuildException, IOException, CreateException, ReadException, WriteException {
        DataSourceManipulator trnDSM = dataManipulators.getPgDataSourceManipulator("transaction_processing",sparkSession, postgres);
        trnDSM.createIfNotExists();
        trnDSM.read(new HashMap<>());
        trnDSM.write(getTrnDataSet(sparkSession), new HashMap<>(), Configuration.ModificationRule.append);
        trnDSM.read(new HashMap<>()).show();
        return trnDSM;
    }

    @Test
    @Order(1)
    void
    testPostgres() throws IOException, CreateException, WriteException, ReadException, DropException, CompactException, TaskFailedException, TaskConfigBuildException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        DataSourceManipulator  clientDSM = createClientDSM(sparkSession);
        long rows = clientDSM.read(new HashMap<>()).count();
        clientDSM.compact(new HashMap<>());
        clientDSM.drop();
        sparkSession.stop();
        assert (rows == 2);
    }


    @Test
    @Order(2)
    void
    testPostgresViolateConstraint() throws IOException, CreateException, ReadException, TaskFailedException, DropException, TaskConfigBuildException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        DataSourceManipulator clientDSM = null;


        try {
            clientDSM = createClientDSM(sparkSession);
        }catch (WriteException e){
            e.printStackTrace();
        }
        //wrong write expect fail by pk
        boolean writeError = false;
        try {
            clientDSM.write(getClientDataSet(sparkSession), new HashMap<>(), Configuration.ModificationRule.append);
        }catch (WriteException e){
            writeError = true;
        }

        clientDSM.read(new HashMap<>()).show();
        clientDSM.drop();
        sparkSession.stop();
        assert (writeError);
    }

    @Test
    @Order(3)
    void
    testPostgresFk() throws IOException, CreateException, WriteException, ReadException, DropException, CompactException, TaskFailedException, TaskConfigBuildException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();
        DataSourceManipulator clientDSM = createClientDSM(sparkSession);
        DataSourceManipulator trnDSM = createTrnDSM(sparkSession);


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

    @Test
    void testIcebergTable() throws TaskFailedException, IOException, CreateException, ReadException, WriteException, TransformationException, TaskConfigBuildException, DropException {
        SparkSession sparkSession = SparkSession.builder().master("local").getOrCreate();

            DataSourceManipulator clientDSM = createClientDSM(sparkSession);
        DataSourceManipulator trnDSM = createTrnDSM(sparkSession);
        DataSourceManipulator trnddsDSM = dataManipulators.getIcebergDataSourceManipulator("transaction_dds",sparkSession);
        TaskProcessorConfigDTO taskProcessorConfigDTO = new TaskConfigTestFactory().loadTaskProcessorConfigDTO(trnddsDSM.getDataSetDTO().getKeyName(),"load");


        BodyParamImpl bodyParam = new BodyParamImpl(
                sparkSession,
                Map.of(
                        trnDSM.getDataSetDTO().getKeyName(), trnDSM,
                        clientDSM.getDataSetDTO().getKeyName(),clientDSM
                ),
                trnddsDSM,
                new HashMap<>(),
                taskProcessorConfigDTO.getScripts(),
                taskProcessorConfigDTO.getKeyBind()
                );
        TransformationSparkProcessorBody tb = new TransformationSparkProcessorBody(bodyParam);
        tb.run();
        /*DataTransformer dt = new SQLDataTransformer(
                taskProcessorConfigDTO.getScripts().get(0),
                SQLDataTransformer.defaultDelimiter);
        Dataset<Row> dataset = dt.transform(Map.of(
           clientDSM.getDataSetDTO().getKeyName(),clientDSM,
           trnDSM.getDataSetDTO().getKeyName(),trnDSM
        ),
        trnddsDSM);
*/
  //      dataset.show();

    //    trnddsDSM.write(dataset, new HashMap<>(), Configuration.ModificationRule.overwrite);
        trnddsDSM.read(new HashMap<>()).show();
        long rows = trnddsDSM.read(new HashMap<>()).count();
        trnDSM.drop();
        clientDSM.drop();
        trnddsDSM.drop();
        assert (rows==3);
    }

    @Test
    @Order(4)
    void tetsDropTableIceberg() throws ReadException, WriteException, TaskFailedException, TaskConfigBuildException, IOException, CreateException, DropException {
        SparkSession sparkSession = SparkSession.builder().master("local[*]").getOrCreate();
        DataSourceManipulator dsm = dataManipulators.getIcebergDataSourceManipulator("transaction_dds",sparkSession);
        boolean isExists = sparkSession.catalog().tableExists( dsm.getCatalogTableFullName());
        dsm.createIfNotExists();
        isExists = sparkSession.catalog().tableExists( dsm.getCatalogTableFullName());
        if ( !isExists )
            throw new CreateException("Table not found");
        dsm.drop();
        isExists = sparkSession.catalog().tableExists( dsm.getCatalogTableFullName());
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

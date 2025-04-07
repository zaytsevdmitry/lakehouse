package org.lakehouse.taskexecutor.executionmodule;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;
//import org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions;
import org.apache.spark.sql.types.StructType;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.taskexecutor.entity.TableDefinition;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//todo this is demo
public class SparkTaskProcessor extends AbstractTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskProcessorConfig taskProcessorConfig;
    public SparkTaskProcessor(
            TaskProcessorConfig taskProcessorConfig) {
        super(taskProcessorConfig);
        this.taskProcessorConfig = taskProcessorConfig;
    }

    SparkSession getSparkSession(String location){
        /*spark-shell --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.8.1\
>         --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
>         --conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog \
>         --conf spark.sql.catalog.spark_catalog.type=hive \
>         --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
>         --conf spark.sql.catalog.local.type=hadoop \
>         --conf spark.sql.catalog.local.warehouse=$PWD/warehouse
**/
        Map<String,Object> conf = new HashMap<>();
        conf.putAll(taskProcessorConfig.getExecutionModuleArgs()
                .entrySet()
                .stream()
                .filter(sse -> sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        conf.put("spark.driver.host", "127.0.0.1");
        conf.put("spark.sql.extensions","org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions");
        conf.put("spark.sql.catalog.spark_catalog","org.apache.iceberg.spark.SparkSessionCatalog");
        conf.put("spark.sql.catalog.spark_catalog.type","hadoop");
        conf.put("spark.sql.catalog.spark_catalog.warehouse",location);
      //  conf.put("spark.hive.metastore.uris","thrift://localhost:9083");
        // conf.put("spark.sql.catalogImplementation","hive");
       // conf.put("spark.sql.hive.metastore.version","3.1.3");
        return SparkSession
                .builder()
                .config(conf)
                .master("local[*]") // todo only for demo
              //  .enableHiveSupport()
                .getOrCreate();


    }

    @Override
    public Status.Task runTask() {
        Map<String,String> keyBind = new HashMap<>();
        keyBind.putAll(taskProcessorConfig.getKeyBind());
        keyBind.putAll(
                taskProcessorConfig.getDataSetDTOSet().stream()
                        .collect(Collectors.toMap(dataSetDTO ->
                                        String.format("${source(%s)}", dataSetDTO.getName()),
                                DataSetDTO::getName))
        );
        taskProcessorConfig.setKeyBind(keyBind);

        
        SparkSession sparkSession = null;
        Status.Task result = null;
        try {



            logger.info("Take script");
            //todo MVP take only one script
            String unfeeledSQL = taskProcessorConfig.getScripts().get(0);

            logger.info("Feel script");
            String sql = feelScripts(unfeeledSQL);

            sparkSession = getSparkSession(
                    taskProcessorConfig
                            .getDataStores()
                            .get(taskProcessorConfig
                                    .getTargetDataSet()
                                    .getDataStore()).getUrl());
            sparkSession.catalog().listCatalogs().show();


            logger.info("Prepare sources");
            prepareSources(sparkSession, taskProcessorConfig.getDataSetDTOSet(),taskProcessorConfig.getDataStores());

            TableDefinition currentTableDefinition =
            taskProcessorConfig.getTableDefinitions().get(taskProcessorConfig.getTargetDataSet().getName());

            logger.info("Creating statement...{}", sql);
            String  merge = String.format(
                    "MERGE INTO %s t   -- a target table\n" +
                            "USING ( %s) q          -- the source updates\n" +
                            "ON %s                -- condition to find updates for target rows\n" +
                            " -- updates\n" +
                            "WHEN MATCHED  THEN UPDATE SET " +
                            "%s\n" +
                            "WHEN NOT MATCHED THEN INSERT  (%s) VALUES (%s)",
                    taskProcessorConfig.getTargetDataSet().getName(),
                    sql,
                    currentTableDefinition.getColumnsMergeOn(),
                    currentTableDefinition.getColumnsUpdateSet(),
                    currentTableDefinition.getColumnsComaSeparated(),
                    currentTableDefinition.getColumnsMergeInsertValues()

            );

            sparkSession.sql(feelScripts("select ( '${target-timestamp-tz}') + interval '1 day' , '${target-timestamp-tz}'")).printSchema();
            sparkSession.sql(feelScripts("select ( '${target-timestamp-tz}') + interval '1 day' , '${target-timestamp-tz}'")).show(false);
            logger.info(merge);
            sparkSession.sql(merge);
            logger.info("Script execution is done");
            sparkSession.sql(String.format("select * from %s",taskProcessorConfig.getTargetDataSet().getName())).show();
            result = Status.Task.SUCCESS;
        }catch (Exception e){
            logger.error("Error task execution",e);
            result = Status.Task.FAILED;
        }finally {
            if(sparkSession == null){
                logger.error("Spark not initialised");
                result = Status.Task.FAILED;
            }else
            {
                logger.info("Shutdown spark");
                sparkSession.stop();
            }

        }
        return result;
    }
    public String feelScripts(String script){
        String result = new String(script);

        for(Map.Entry<String,String> sse: taskProcessorConfig
                .getKeyBind()
                .entrySet()){
            logger.info("Replace {} to {}",sse.getKey(),sse.getValue());
            result = result.replace(sse.getKey(), sse.getValue());
        }
        logger.info("Prepared query looks as {}", result);
        return result;
    }
    /*private void resolveTable(SparkSession sparkSession, TableDefinition tableDefinition){
        if (sparkSession
                    .sql("show tables")
                    .filter(String.format(
                            "(namespace = '%s' or '%s' =='') and tableName='%s'",
                            tableDefinition.getSchemaName(),
                            tableDefinition.getSchemaName(),
                            tableDefinition.getFullTableName()
                    )).count() < 1)
        {
            sparkSession.sql(tableDefinition.getTableDDL());
        }
    }*/

    private void prepareSources(
            SparkSession sparkSession,
            Set<DataSetDTO> datasets,
            Map<String,DataStoreDTO> dataStoreDTOMap){
       /// sparkSession.sql("show tables").show();
        for(DataSetDTO dataSetDTO:datasets){
           // sparkSession.sql("show tables").show();
            DataStoreDTO dataStoreDTO = dataStoreDTOMap.get(dataSetDTO.getDataStore());
                if(dataStoreDTO.getInterfaceType().equals("jdbc")){
                    sparkSession
                            .read()
                            .format("jdbc")
                            .options(dataStoreDTO.getProperties())
                            .option("url", dataStoreDTO.getUrl())
                            .option("dbtable", dataSetDTO.getFullTableName())
                            .load()
                            .createOrReplaceTempView(dataSetDTO.getName());

                }else{
                    //todo just for demo
                    TableDefinition td = taskProcessorConfig.getTableDefinitions().get(dataSetDTO.getName());
                  //  sparkSession.sqlContext().tables().show();
                   /* sparkSession.sqlContext().createExternalTable(
                            dataSetDTO.getName(),
                            "iceberg", //todo add this to dataset config
                            StructType.fromDDL(td.getColumnsDDL()),dataStoreDTO.getProperties());
*/
                   /* fscheck(sparkSession,dataStoreDTO.getUrl().concat(dataSetDTO.getProperties().get("location")));
               */  /*  sparkSession
                            .read()
                            .format("iceberg")
                            .schema(td.getColumnsDDL())
                            .load(dataStoreDTO.getUrl().concat(dataSetDTO.getProperties().get("location")))
                            //.parquet(dataStoreDTO.getUrl().concat(dataSetDTO.getProperties().get("location")))
                            .createOrReplaceTempView(dataSetDTO.getName());*/

                String ddl = String.format("create  external table\n if not exists\n %s(%s)\n using  iceberg\n",
                        dataSetDTO.getName(),
                        td.getColumnsDDL()
                      //  dataStoreDTO.getUrl().concat(dataSetDTO.getProperties().get("location"))
                        );
                logger.info(ddl);
                sparkSession.sql(ddl);
                }

            logger.info(dataSetDTO.getName());
            sparkSession.sql(String.format("select * from %s",dataSetDTO.getName())).show();
        }

        /*
        taskProcessorConfig
                .getTableDefinitions()
                .values()
                .forEach(t -> {
                    logger.info(t.getTableDDL());
                    sparkSession.sql(t.getTableDDL());
                });

*/
    }
    private void fscheck(SparkSession sparkSession, String location){
        try {
            Path path = new Path(location);
            FileSystem fs = path.getFileSystem(new Configuration());
            if (!fs.exists(path))
                fs.mkdirs(path);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }

}

/*

        spark-shell --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.8.1\
                 --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
                 --conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog \
                 --conf spark.sql.catalog.spark_catalog.type=hive \
                 --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
                 --conf spark.sql.catalog.local.type=hadoop \
                 --conf spark.sql.catalog.local.warehouse=$PWD/warehouse \
                 --conf spark.hive.metastore.uris="thrift://localhost:9083"

*/
//val spark = SparkSession.builder().appName("Spark Hive Example").enableHiveSupport().getOrCreate()
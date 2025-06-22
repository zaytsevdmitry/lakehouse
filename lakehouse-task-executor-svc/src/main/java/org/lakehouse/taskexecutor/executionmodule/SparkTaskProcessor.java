package org.lakehouse.taskexecutor.executionmodule;

import com.hubspot.jinjava.Jinjava;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.taskexecutor.entity.TableDefinition;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

//todo this is demo. ad-hoc experimental  code
public class SparkTaskProcessor extends AbstractDefaultTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
   
    public SparkTaskProcessor(
            TaskProcessorConfig taskProcessorConfig, Jinjava jinjava) {
        super(taskProcessorConfig, jinjava);
    }

    SparkSession getSparkSession(String location){
        /*

        spark-shell --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.8.1\
>         --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
>         --conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog \
>         --conf spark.sql.catalog.spark_catalog.type=hive \
>         --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
>         --conf spark.sql.catalog.local.type=hadoop \
>         --conf spark.sql.catalog.local.warehouse=$PWD/warehouse
**/

        Map<String,Object> conf = new HashMap<>();
        conf.putAll(getTaskProcessorConfig().getExecutionModuleArgs()
                .entrySet()
                .stream()
                .filter(sse -> sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        conf.put("spark.sql.extensions","org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions");
        conf.put("spark.sql.catalog.spark_catalog","org.apache.iceberg.spark.SparkSessionCatalog");
        conf.put("spark.sql.catalog.spark_catalog.type","hadoop");
        conf.put("spark.sql.catalog.spark_catalog.warehouse",location);
        conf.forEach((string, o) -> logger.info("Spark configuration changes {} -> {}",string,o));
        return SparkSession
                .builder()
                .config(conf)
                .master("local[1]") // todo only for demo
                .getOrCreate();


    }

    @Override
    public void runTask() throws TaskFailedException {
        Map<String,String> keyBind = new HashMap<>();
        keyBind.putAll(getTaskProcessorConfig().getKeyBind());
        keyBind.putAll(
                getTaskProcessorConfig().getDataSetDTOSet().stream()
                        .collect(Collectors.toMap(dataSetDTO ->
                                        String.format("${source(%s)}", dataSetDTO.getKeyName()),
                                DataSetDTO::getKeyName))
        );
        getTaskProcessorConfig().setKeyBind(keyBind);
        SparkSession sparkSession = null;
        try {



            logger.info("Take script");
            //todo MVP take only one script
            String unfeeledSQL = getJinjava()
                    .render(
                            getTaskProcessorConfig().getScripts().get(0),
                            getTaskProcessorConfig().getKeyBind());


            logger.info("Feel script");
            String sql = feelScripts(unfeeledSQL);

            sparkSession = getSparkSession(
                    getTaskProcessorConfig()
                            .getDataStores()
                            .get(getTaskProcessorConfig()
                                    .getTargetDataSet()
                                    .getDataStoreKeyName()).getUrl());
            sparkSession.catalog().listCatalogs().show();


            logger.info("Prepare sources");
            prepareSources(sparkSession, getTaskProcessorConfig().getDataSetDTOSet(),getTaskProcessorConfig().getDataStores());

            TableDefinition currentTableDefinition =
            getTaskProcessorConfig().getTableDefinitions().get(getTaskProcessorConfig().getTargetDataSet().getKeyName());



            logger.info("Creating statement...{}", sql);
                String  merge = String.format(
                    "MERGE INTO %s t   -- a target table\n" +
                            "USING ( %s) q          -- the source updates\n" +
                            "ON %s                -- condition to find updates for target rows\n" +
                            " -- updates\n" +
                            "WHEN MATCHED  THEN UPDATE SET " +
                            "%s\n" +
                            "WHEN NOT MATCHED THEN INSERT  (%s) VALUES (%s)",
                    getTaskProcessorConfig().getTargetDataSet().getKeyName(),
                    sql,
                    currentTableDefinition.getColumnsMergeOn(),
                    currentTableDefinition.getColumnsUpdateSet(),
                    currentTableDefinition.getColumnsComaSeparated(),
                    currentTableDefinition.getColumnsMergeInsertValues()

            );

            //sparkSession.sql(feelScripts("select ( '{{ targetDateTimeTZ }}') + interval '1 day' , '{{ targetDateTimeTZ }}'")).printSchema();
            //sparkSession.sql(feelScripts("select ( '{{ targetDateTimeTZ }}') + interval '1 day' , '{{ targetDateTimeTZ }}'")).show(false);
            logger.info(merge);
            sparkSession.sql(merge);
            logger.info("Script execution is done");
            sparkSession.sql(String.format("select * from %s",getTaskProcessorConfig().getTargetDataSet().getKeyName())).show();
        }catch (Exception e){
            logger.error("Error task execution",e);
            throw new TaskFailedException(e);
        }finally {
            if(sparkSession == null){
                logger.error("Spark not initialised");
            }else
            {
                logger.info("Shutdown spark");
                sparkSession.stop();
            }
            if (!SparkSession.getActiveSession().isEmpty()){
                SparkSession s = SparkSession.getActiveSession().get();
                if (s != null)
                    s.close();
            }

        }
    }
    public String feelScripts(String script){
        String result = new String(script);

        for(Map.Entry<String,String> sse: getTaskProcessorConfig()
                .getKeyBind()
                .entrySet()){
            logger.info("Replace {} to {}",sse.getKey(),sse.getValue());
            result = result.replace(sse.getKey(), sse.getValue());
        }
        logger.info("Prepared query looks as {}", result);
        return result;
    }


    private void prepareSources(
            SparkSession sparkSession,
            Set<DataSetDTO> datasets,
            Map<String,DataStoreDTO> dataStoreDTOMap){
        for(DataSetDTO dataSetDTO:datasets){
            DataStoreDTO dataStoreDTO = dataStoreDTOMap.get(dataSetDTO.getDataStoreKeyName());
                if(dataStoreDTO.getInterfaceType().equals("jdbc")){
                    Properties properties = new Properties();
                    properties.putAll(dataStoreDTO.getProperties());
                    sparkSession
                            .read()
                            .jdbc(dataStoreDTO.getUrl(),dataSetDTO.getFullTableName(),properties)
                            .createOrReplaceTempView(dataSetDTO.getKeyName());

                }else{
                    //todo just for demo
                    TableDefinition td = getTaskProcessorConfig().getTableDefinitions().get(dataSetDTO.getKeyName());

                String ddl = String.format("create  external table\n if not exists\n %s(%s)\n using  iceberg\n",
                        dataSetDTO.getKeyName(),
                        td.getColumnsDDL());
                logger.info(ddl);
                sparkSession.sql(ddl);
                }

            logger.info(dataSetDTO.getKeyName());
            sparkSession.sql(String.format("select * from %s",dataSetDTO.getKeyName())).show();
        }
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
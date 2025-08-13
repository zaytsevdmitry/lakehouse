package org.lakehouse.taskexecutor.executionmodule.body;


import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.common.api.task.processor.entity.TableDefinition;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.lakehouse.common.api.task.processor.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

//todo this is demo. ad-hoc experimental  code
public class SparkTaskProcessorBody extends SparkProcessorBodyAbstract{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public SparkTaskProcessorBody(
            SparkSession sparkSession,
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(sparkSession,taskProcessorConfigDTO);
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
        conf.putAll(getTaskProcessorConfigDTO().getExecutionModuleArgs()
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
                .getOrCreate();
    }

    @Override
    public void run() throws TaskFailedException {

        SparkSession sparkSession = null;
        try {

            logger.info("Take script");
            String sql = getTaskProcessorConfigDTO().getScripts().get(0); //feelScripts(unfeeledSQL);

            sparkSession = getSparkSession(
                    getTaskProcessorConfigDTO()
                            .getDataStores()
                            .get(getTaskProcessorConfigDTO()
                                    .getTargetDataSet()
                                    .getDataStoreKeyName()).getUrl());
            sparkSession.catalog().listCatalogs().show();


            logger.info("Prepare sources");
            prepareSources(sparkSession, getTaskProcessorConfigDTO().getDataSetDTOSet(),getTaskProcessorConfigDTO().getDataStores());

            TableDefinition currentTableDefinition =
            getTaskProcessorConfigDTO().getTableDefinitions().get(getTaskProcessorConfigDTO().getTargetDataSet().getKeyName());



            logger.info("Creating statement...{}", sql);
                String  merge = String.format(
                    "MERGE INTO %s t   -- a target table\n" +
                            "USING ( %s) q          -- the source updates\n" +
                            "ON %s                -- condition to find updates for target rows\n" +
                            " -- updates\n" +
                            "WHEN MATCHED  THEN UPDATE SET " +
                            "%s\n" +
                            "WHEN NOT MATCHED THEN INSERT  (%s) VALUES (%s)",
                    getTaskProcessorConfigDTO().getTargetDataSet().getKeyName(),
                    sql,
                    currentTableDefinition.getColumnsMergeOn(),
                    currentTableDefinition.getColumnsUpdateSet(),
                    currentTableDefinition.getColumnsComaSeparated(),
                    currentTableDefinition.getColumnsMergeInsertValues()

            );

           logger.info(merge);
            sparkSession.sql(merge);
            logger.info("Script execution is done");
            sparkSession.sql(String.format("select * from %s",getTaskProcessorConfigDTO().getTargetDataSet().getKeyName())).show();
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
                    TableDefinition td = getTaskProcessorConfigDTO().getTableDefinitions().get(dataSetDTO.getKeyName());

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
/*    private void fscheck(SparkSession sparkSession, String location){
        try {
            Path path = new Path(location);
            FileSystem fs = path.getFileSystem(new Configuration());
            if (!fs.exists(path))
                fs.mkdirs(path);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            throw new RuntimeException(e);
        }
    }*/

}

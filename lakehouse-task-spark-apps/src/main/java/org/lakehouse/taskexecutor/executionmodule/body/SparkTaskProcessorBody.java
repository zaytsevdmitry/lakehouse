package org.lakehouse.taskexecutor.executionmodule.body;


import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.task.TableDefinition;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

//todo this is demo. ad-hoc experimental  code
public class SparkTaskProcessorBody extends SparkProcessorBodyAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SparkTaskProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }


    SparkSession getSparkSession(String location) {
        /*

        spark-shell --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.8.1\
>         --conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
>         --conf spark.sql.catalog.spark_catalog=org.apache.iceberg.spark.SparkSessionCatalog \
>         --conf spark.sql.catalog.spark_catalog.type=hive \
>         --conf spark.sql.catalog.local=org.apache.iceberg.spark.SparkCatalog \
>         --conf spark.sql.catalog.local.type=hadoop \
>         --conf spark.sql.catalog.local.warehouse=$PWD/warehouse
**/

        Map<String, Object> conf = new HashMap<>();
        conf.putAll(getTaskProcessorConfigDTO().getExecutionModuleArgs()
                .entrySet()
                .stream()
                .filter(sse -> sse.getValue().startsWith("spark."))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        conf.put("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions");
        conf.put("spark.sql.catalog.spark_catalog", "org.apache.iceberg.spark.SparkSessionCatalog");
        conf.put("spark.sql.catalog.spark_catalog.type", "hadoop");
        conf.put("spark.sql.catalog.spark_catalog.warehouse", "./wh");

        conf.forEach((string, o) -> logger.info("Spark configuration changes {} -> {}", string, o));
        return SparkSession
                .builder()
                .config(conf)
                .getOrCreate();
    }

    private String buildDbUrl(DataSourceDTO dataSourceDTO) throws TaskFailedException {
        String result = null;
        ServiceDTO serviceDTO;
        if (dataSourceDTO.getServices().isEmpty())
            throw new TaskFailedException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        else
            serviceDTO = dataSourceDTO.getServices().get(0);

        if (!dataSourceDTO.getDataSourceType().equals(Types.DataSourceType.database))
            throw new TaskFailedException(String.format("DataSource %s is not database", dataSourceDTO.getKeyName()));
        else if (dataSourceDTO.getDataSourceServiceType().equals(Types.DataSourceServiceType.postgres)) {
            result = String.format("jdbc:postgresql://%s:%s/%s", serviceDTO.getHost(), serviceDTO.getPort(), serviceDTO.getUrn());
        } else if (dataSourceDTO.getDataSourceServiceType().equals(Types.DataSourceServiceType.trino)) {
            result = String.format("jdbc:trino://%s:%s/%s", serviceDTO.getHost(), serviceDTO.getPort(), serviceDTO.getUrn());
        } else {
            throw new TaskFailedException(String.format("DataSource %s with database %s not supported", dataSourceDTO.getKeyName(), dataSourceDTO.getDataSourceServiceType()));
        }
        return result;
    }

    @Override
    public void run() throws TaskFailedException {

        SparkSession sparkSession = null;
        try {

            logger.info("Take script");
            String sql = getTaskProcessorConfigDTO().getScripts().get(0); //feelScripts(unfeeledSQL);

            String location = null;
            DataSourceDTO targetSource = getTaskProcessorConfigDTO().getDataSources().get(
                    getTaskProcessorConfigDTO().getTargetDataSet().getDataSourceKeyName());
            if (!targetSource.getDataSourceType().equals(Types.DataSourceType.filesystem))
                throw new TaskFailedException("Wrong type of dataSource");
            else if (targetSource.getDataSourceServiceType().equals(Types.DataSourceServiceType.localfs)) {
                location = targetSource.getServices().get(0).getUrn();
            } else throw new TaskFailedException("Wrong File system service");
            sparkSession = getSparkSession(location);

            for (Tuple2<String, String> entry : sparkSession.sparkContext().getConf().getAll()) {
                System.out.println(entry._1() + " = " + entry._2());
            }
            sparkSession.catalog().listCatalogs().show();


            logger.info("Prepare sources");
            prepareSources(sparkSession, getTaskProcessorConfigDTO().getDataSetDTOSet(), getTaskProcessorConfigDTO().getDataSources());

            TableDefinition currentTableDefinition =
                    getTaskProcessorConfigDTO().getTableDefinitions().get(getTaskProcessorConfigDTO().getTargetDataSet().getKeyName());


            logger.info("Creating statement...{}", sql);
            String merge = String.format(
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
            sparkSession.sql(String.format("select * from %s", getTaskProcessorConfigDTO().getTargetDataSet().getKeyName())).show();
        } catch (Exception e) {
            logger.error("Error task execution", e);
            throw new TaskFailedException(e);
        } finally {
            if (sparkSession == null) {
                logger.error("Spark not initialised");
            } else {
                logger.info("Shutdown spark");
                sparkSession.stop();
            }
            if (!SparkSession.getActiveSession().isEmpty()) {
                SparkSession s = SparkSession.getActiveSession().get();
                if (s != null)
                    s.close();

            }

        }
    }

    private void prepareSources(
            SparkSession sparkSession,
            Set<DataSetDTO> datasets,
            Map<String, DataSourceDTO> dataStoreDTOMap) throws ClassNotFoundException, TaskFailedException {
        for (DataSetDTO dataSetDTO : datasets) {
            DataSourceDTO dataSourceDTO = dataStoreDTOMap.get(dataSetDTO.getDataSourceKeyName());
            if (dataSourceDTO.getDataSourceType().equals(Types.DataSourceType.database)) {
                String jdbcUrl = buildDbUrl(dataSourceDTO);
                logger.info("jdbc url {} user {} password {}",
                        jdbcUrl,
                        dataSourceDTO.getProperties().get("user"),
                        dataSourceDTO.getProperties().get("password"));
                Class.forName("org.postgresql.Driver");
                Properties properties = new Properties();
                properties.putAll(dataSourceDTO.getServices().get(0).getProperties());
                sparkSession
                        .read()
                        .jdbc(jdbcUrl, dataSetDTO.getFullTableName(), properties)
                        .createOrReplaceTempView(dataSetDTO.getKeyName());

            } else {
                //todo just for demo
                TableDefinition td = getTaskProcessorConfigDTO().getTableDefinitions().get(dataSetDTO.getKeyName());

                String ddl = String.format("create  external table\n if not exists\n %s(%s)\n using  iceberg\n",
                        dataSetDTO.getKeyName(),
                        td.getColumnsDDL());
                logger.info(ddl);
                sparkSession.sql(ddl);
            }

            logger.info(dataSetDTO.getKeyName());
            sparkSession.sql(String.format("select * from %s", dataSetDTO.getKeyName())).show();
        }
    }
}

spark-shell --master local --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.9.2 \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
--conf spark.sql.catalog.lakehouse=org.apache.iceberg.spark.SparkCatalog \
--conf spark.sql.catalog.lakehouse.type="hadoop" \
--conf spark.sql.catalog.lakehouse.warehouse="$PWD/warehouse"

spark.history.fs.logDirectory


spark-shell --master local \
  --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.9.2,org.apache.spark:spark-sql-kafka-0-10_2.12:3.5.0 \
--conf "spark.driver.extraJavaOptions"="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -XX:+IgnoreUnrecognizedVMOptions" \
--conf "spark.executor.extraJavaOptions"="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -XX:+IgnoreUnrecognizedVMOptions" \
--conf "spark.sql.extensions"="org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions" \
--conf "spark.sql.catalog.lakehouse"="org.apache.iceberg.spark.SparkCatalog" \
--conf "spark.sql.catalog.lakehouse.type"="hive" \
--conf "spark.sql.catalog.lakehouse.uri"="thrift://localhost:9083" \
--conf "spark.sql.catalogImplementation"="hive" \
--conf "spark.hadoop.fs.s3a.endpoint"="http://localhost:9000" \
--conf "spark.hive.s3.endpoint"="http://localhost:9000" \
--conf "spark.hadoop.fs.s3a.access.key"="spark_user" \
--conf "spark.hadoop.fs.s3a.secret.key"="spark_pwd" \
--conf "spark.hadoop.fs.s3a.path.style.access"="true" \
--conf "spark.sql.catalog.spark_catalog.warehouse"="s3a://data/warehouse" \
--conf "spark.hadoop.fs.s3a.impl"="org.apache.hadoop.fs.s3a.S3AFileSystem" \
--conf "spark.sql.catalog.processing"="org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog" \
--conf "spark.sql.catalog.processing.user"="postgresUser" \
--conf "spark.sql.catalog.processing.password"="postgresPW" \
--conf "spark.sql.catalog.processing.type"="hive" \
--conf "spark.sql.catalog.processing.url"="jdbc:postgresql://localhost:5432/postgresDB"


spark.catalog.setCurrentCatalog("processing")
spark.catalog.setCurrentCatalog("lakehouse")
spark.sql("""
select '' subMetricName
     , count(1) value
from lakehouse.default.transaction_dds
where reg_date_time >= timestamp '2025-01-01T00:00:00Z'
  and reg_date_time <  timestamp '2025-01-02T00:00:00Z'
""").show

--conf "spark.sql.catalog.processing.url": "{{driver.connectionTemplates['jdbc']}}"

spark-shell --master local \
--conf "spark.sql.catalog.processing"="org.apache.spark.sql.execution.datasources.v2.jdbc.JDBCTableCatalog" \
--conf "spark.sql.catalog.processing.user"="postgresUser" \
--conf "spark.sql.catalog.processing.password"="postgresUser" \
--conf "spark.sql.catalog.processing.type"="hive" \
--conf "spark.sql.catalog.processing.url"="jdbc:postgresql://localhost:5432/postgresDB"

--conf "spark.sql.catalogImplementation"="hive"

export AWS_ACCESS_KEY_ID=spark_user
export AWS_SECRET_ACCESS_KEY=spark_pwd

spark.sql("set 'spark.sql.catalog.spark_catalog.warehouse'='s3a://data/warehouse'").show

spark.sql("""
CREATE TABLE lakehousestorage.default.transaction_dds (
id bigint,
amount decimal,
client_id string,
client_name string,
commission string,
provider_id string,
reg_date_time timestamp
)
USING iceberg
LOCATION 's3a://data/warehouse/default/transaction_dds'
""").show


spark.sql("""
CREATE CATALOG procprocjopa WITH (
  'type' = 'hive',
  'hive-conf-dir' = '/opt/apache-hive-metastore-3.1.3-bin/conf/' -- Directory containing hive-site.xml
)
""").show


spark.sql("""
select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from processingdb.proc.transactions t   -- refCat returns table name with catalog
join processingdb.proc.client c
  on t.client_id = c.id
""").writeTo("lakehousestorage.default.transaction_dds")
      spark.sql("""
      insert into  lakehousestorage.default.transaction_dds (
      id,reg_date_time,client_id,client_name,provider_id,amount,commission
      )
      select t.id id
           , t.reg_date_time
           , c.id   as client_id
           , c.name as client_name
           , t.provider_id
           , t.amount
           , t.commission
      from processingdb.proc.transactions t   -- refCat returns table name with catalog
      join processingdb.proc.client c
        on t.client_id = c.id
      """).show


spark.sql("select * from lakehouse.default.transaction_dds").show


spark.sql("""
select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from processing.proc.transactions t   -- refCat returns table name with catalog
join processing.proc.client c
  on t.client_id = c.id
 where
   t.reg_date_time >= timestamp '2025-01-01T00:00:00Z' and
   t.reg_date_time < timestamp '2025-01-02T00:00:00Z'
""").show



spark.sql("""
MERGE INTO  lakehouse.default.transaction_dds t
	USING (select t.id id
     , t.reg_date_time
     , c.id   as client_id
     , c.name as client_name
     , t.provider_id
     , t.amount
     , t.commission
from processing.proc.transactions t   -- refCat returns table name with catalog
join processing.proc.client c
  on t.client_id = c.id
 where
   t.reg_date_time >= timestamp '2025-01-01T00:00:00Z' and
   t.reg_date_time < timestamp '2025-01-02T00:00:00Z'
-- NB spark sql
) q
	ON (t.id = q.id)
	WHEN MATCHED THEN
		UPDATE SET 			amount = q.amount,
			client_id = q.client_id,
			client_name = q.client_name,
			commission = q.commission,
			provider_id = q.provider_id,
			reg_date_time = q.reg_date_time
	WHEN NOT MATCHED BY TARGET THEN
		INSERT(id,amount,client_id,client_name,commission,provider_id,reg_date_time)
		VALUES (q.id,q.amount,q.client_id,q.client_name,q.commission,q.provider_id,q.reg_date_time)
""").show
spark-shell --master local --packages org.apache.iceberg:iceberg-spark-runtime-3.5_2.12:1.9.2 \
--conf spark.sql.extensions=org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions \
--conf spark.sql.catalog.lakehousestorage=org.apache.iceberg.spark.SparkCatalog \
--conf spark.sql.catalog.lakehousestorage.type="hadoop" \
--conf spark.sql.catalog.lakehousestorage.warehouse="$PWD/warehouse"
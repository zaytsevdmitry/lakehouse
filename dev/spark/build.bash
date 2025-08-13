  #!/usr/bin/env bash
set -e
export VERSION=0.3.0

cp ../../lakehouse-task-spark-apps/target/lakehouse-task-spark-apps-$VERSION-jar-with-dependencies.jar ./
#wget https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.5_2.12/1.9.2/iceberg-spark-runtime-3.5_2.12-1.9.2.jar

docker build -t lakehouse-spark:$VERSION ./
docker images
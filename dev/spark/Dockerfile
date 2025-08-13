FROM bitnami/spark:3.5

ENV LHVERSION=0.3.0
ENV SPARK_VERSION=3.5
COPY lakehouse-task-spark-apps-$LHVERSION-jar-with-dependencies.jar /opt/bitnami/spark/jars/
COPY iceberg-spark-runtime-3.5_2.12-1.9.2.jar /opt/bitnami/spark/jars/


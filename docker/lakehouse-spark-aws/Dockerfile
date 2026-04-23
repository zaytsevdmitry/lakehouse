FROM eclipse-temurin:17

RUN apt-get update && apt-get install -y wget tar unzip bash mc

ENV SCALA_VERSION=2.12 \
    SPARK_MAJOR_VERSION=3.5 \
    SPARK_VERSION=3.5.8 \
    HADOOP_VERSION=3.3.4 \
    ICEBERG_VERSION=1.9.2 \
    AWS_SDK_VERSION=1.12.262 \
    PSQL_CONN_VERSION=42.7.8 \
    SPARK_HOME=/opt/spark \
    HADOOP_HOME=/opt/spark \
    PATH="${SPARK_HOME}/bin:${SPARK_HOME}/sbin:/usr/local/bin:${PATH}" \
    APP_HOME=/opt/lakehouse-task-spark-apps \
    JAVA_OPTS="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED"


WORKDIR /tmp

# 1. Spark & Cleanup
RUN wget -nv https://dlcdn.apache.org/spark/spark-${SPARK_VERSION}/spark-${SPARK_VERSION}-bin-hadoop3.tgz && \
    mkdir -p /opt/spark && \
    tar -xzf spark-${SPARK_VERSION}-bin-hadoop3.tgz -C /opt/spark --strip-components=1 && \
    rm spark-${SPARK_VERSION}-bin-hadoop3.tgz




# 2. Drivers (AWS S3A & Postgres)
RUN wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/postgresql/postgresql/${PSQL_CONN_VERSION}/postgresql-${PSQL_CONN_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-${SPARK_MAJOR_VERSION}_${SCALA_VERSION}/${ICEBERG_VERSION}/iceberg-spark-runtime-${SPARK_MAJOR_VERSION}_$SCALA_VERSION-${ICEBERG_VERSION}.jar

# 3. AWS CLI
RUN wget -nv https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip && \
    unzip awscli-exe-linux-x86_64.zip -d /opt/aws_installer && \
    /opt/aws_installer/aws/install && \
    rm awscli-exe-linux-x86_64.zip && \
    rm -rf /opt/aws_installer


LABEL type="lakehouse=spark" \
      version="lakehouse-0.3.0_spark-3.5.8" \
      features="spark, aws-s3a, postgres-jdbc, aws-cli"

RUN apt-get update && \
    apt-get install -y --no-install-recommends bash procps groff less mc && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${APP_HOME}
ADD lakehouse-task-executor-spark-dataset-app-0.3.0-jar-with-dependencies.jar ${APP_HOME}/
ADD lakehouse-task-executor-spark-dq-app-0.3.0-jar-with-dependencies.jar ${APP_HOME}/
RUN userdel -r ubuntu  && \
    groupadd -r spark  && \
    useradd -r -g spark --uid=1000 -d /home/spark -m spark && \
    chown -R spark:spark ${SPARK_HOME} ${APP_HOME}


ADD entrypoint.sh /
ENTRYPOINT /entrypoint.sh
USER spark
WORKDIR /home/spark

EXPOSE 4040 9083


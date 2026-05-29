FROM spark:3.5.8-scala2.12-java17-ubuntu

USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends bash procps groff less wget tar unzip  mc && \
    rm -rf /var/lib/apt/lists/*
ADD service.sh /

RUN chown spark:spark /service.sh && chmod +x /service.sh

ADD log4j2.properties /opt/spark/conf/
RUN chown spark:spark /opt/spark/conf/log4j2.properties && chmod +x /opt/spark/conf/log4j2.properties
RUN rm -f /opt/spark/jars/guava-*.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/com/google/guava/guava/33.6.0-jre/guava-33.6.0-jre.jar

# 3. AWS CLI
RUN wget -nv https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip && \
    unzip awscli-exe-linux-x86_64.zip -d /opt/aws_installer && \
    /opt/aws_installer/aws/install && \
    rm awscli-exe-linux-x86_64.zip && \
    rm -rf /opt/aws_installer
ENV APP_HOME=/opt/lakehouse-task-spark-apps
RUN mkdir -p ${APP_HOME}

USER spark




ENV SCALA_VERSION=2.12 \
    SPARK_MAJOR_VERSION=3.5 \
    SPARK_VERSION=3.5.8 \
    HADOOP_VERSION=3.3.4 \
    ICEBERG_VERSION=1.9.2 \
    AWS_SDK_VERSION=1.12.262 \
    PSQL_CONN_VERSION=42.7.8 \
    LOG_APPENDER_S3_VERSION=5.6.0 \
    APP_HOME=/opt/lakehouse-task-spark-apps \
    JAVA_OPTS="--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED"


WORKDIR /tmp


#  Drivers (AWS S3A & Postgres)
RUN wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_VERSION}/hadoop-aws-${HADOOP_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/postgresql/postgresql/${PSQL_CONN_VERSION}/postgresql-${PSQL_CONN_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-${SPARK_MAJOR_VERSION}_${SCALA_VERSION}/${ICEBERG_VERSION}/iceberg-spark-runtime-${SPARK_MAJOR_VERSION}_$SCALA_VERSION-${ICEBERG_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/com/therealvan/appender-log4j2/${LOG_APPENDER_S3_VERSION}/appender-log4j2-${LOG_APPENDER_S3_VERSION}.jar && \
    wget -nv -P /opt/spark/jars/ https://repo1.maven.org/maven2/com/therealvan/appender-core/${LOG_APPENDER_S3_VERSION}/appender-core-${LOG_APPENDER_S3_VERSION}.jar




LABEL type="lakehouse=spark" \
      version="lakehouse-0.4.0_spark-3.5.8" \
      features="spark, aws-s3a, postgres-jdbc, aws-cli"



ADD lakehouse-task-executor-spark-dataset-app-0.4.0-jar-with-dependencies.jar ${APP_HOME}/
ADD lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar ${APP_HOME}/

EXPOSE 4040 9083


#!/usr/bin/env bash
source ../bashfunctions.bash
set -e
export LH_VERSION=0.4.0
export HADOOP_VERSION=3.3.4
export METASTORE_VERSION=3.1.3
export PSQL_CONN_VERSION=42.3.1
export LOG4J_WEB_VERSION=2.17.1
export RM_HADOOP_LOG4J_VERSION=1.7.30
export HADOOP_HOME=./opt/hadoop-${HADOOP_VERSION}
export HIVE_HOME=./opt/apache-hive-metastore-${METASTORE_VERSION}-bin
export HADOOP_CLASSPATH=${HADOOP_HOME}/share/hadoop/tools/lib/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar:${HADOOP_HOME}/share/hadoop/tools/lib/hadoop-aws-${HADOOP_VERSION}.jar
export ENTRYPOINT_FILE=entrypoint.sh
mkdir -p opt
mkdir -p $HADOOP_HOME
mkdir -p $HIVE_HOME


downloadIfNotExists hadoop-${HADOOP_VERSION}.tar.gz \
                    https://archive.apache.org/dist/hadoop/common/hadoop-${HADOOP_VERSION} \
                    "./" \
                    "http"

tar xzvf  hadoop-${HADOOP_VERSION}.tar.gz -C ./opt
rm -rf opt/hadoop-3.3.4/share/doc

downloadIfNotExists hive-standalone-metastore-${METASTORE_VERSION}-bin.tar.gz \
                    https://repo1.maven.org/maven2/org/apache/hive/hive-standalone-metastore/${METASTORE_VERSION} \
                    "./" \
                    "http"

tar xzvf hive-standalone-metastore-${METASTORE_VERSION}-bin.tar.gz -C ./opt


downloadIfNotExists hive-exec-${METASTORE_VERSION}.jar \
                    https://repo1.maven.org/maven2/org/apache/hive/hive-exec/${METASTORE_VERSION} \
                    "${HIVE_HOME}/lib/" \
                    "http"

                    downloadIfNotExists log4j-web-${LOG4J_WEB_VERSION}.jar \
                    https://repo1.maven.org/maven2/org/apache/logging/log4j/log4j-web/${LOG4J_WEB_VERSION} \
                    "${HIVE_HOME}/lib/" \
                    "http"

downloadIfNotExists postgresql-42.7.8.jar \
                    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.8 \
                    "${HIVE_HOME}/lib/" \
                    "http"

downloadIfNotExists hadoop-aws-3.3.4.jar \
                    https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.3.4 \
                    "$HADOOP_HOME/share/hadoop/common" \
                    "http"

downloadIfNotExists aws-java-sdk-bundle-1.12.262.jar \
                    https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.12.262 \
                    "$HADOOP_HOME/share/hadoop/common" \
                    "http"

downloadIfNotExists wildfly-openssl-1.0.7.Final.jar \
                    https://repo1.maven.org/maven2/org/wildfly/openssl/wildfly-openssl/1.0.7.Final \
                    "$HADOOP_HOME/share/hadoop/common" \
                    "http"


export ENTRYPOINT_FILE=entrypoint.sh

echo "FROM eclipse-temurin:17" > Dockerfile
echo "ENV DATABASE_TYPE=postgres">> Dockerfile
echo "ADD opt /opt">> Dockerfile
echo "ENV HADOOP_VERSION=$HADOOP_HADOOP_VERSION">> Dockerfile
echo "ENV METASTORE_VERSION=$METASTORE_VERSION">> Dockerfile
echo "ENV HADOOP_HOME=/opt/hadoop-$HADOOP_VERSION">> Dockerfile
echo "ENV HADOOP_CLASSPATH=/opt/hadoop-$HADOOP_VERSION/share/hadoop/common/hadoop-aws-3.3.4.jar:/opt/hadoop-$HADOOP_VERSION/share/hadoop/common/aws-java-sdk-bundle-1.12.262.jar:/opt/hadoop-$HADOOP_VERSION/share/hadoop/common/wildfly-openssl-1.0.7.Final.jar" >>Dockerfile
echo "ENV HIVE_HOME=/opt/apache-hive-metastore-$METASTORE_VERSION-bin">> Dockerfile
echo "ADD ./$ENTRYPOINT_FILE /">> Dockerfile
echo "ENV ENTRYPOINT_FILE=$ENTRYPOINT_FILE" >> Dockerfile
echo "RUN chmod +x $ENTRYPOINT_FILE">> Dockerfile
echo "RUN groupadd -r hive --gid=1001 && \ ">> Dockerfile
echo "    useradd -r -g hive --uid=1001 -d /opt/hadoop-$HADOOP_VERSION hive && \ ">> Dockerfile
echo "    chown hive:hive -R /opt/apache-hive-metastore-$METASTORE_VERSION-bin">> Dockerfile
echo "USER hive">> Dockerfile
echo "EXPOSE 9083">> Dockerfile
echo "ENTRYPOINT /$ENTRYPOINT_FILE">> Dockerfile
echo "">> Dockerfile


docker build -t lakehouse-hms:$LH_VERSION ./
docker images
rm -rf ./opt
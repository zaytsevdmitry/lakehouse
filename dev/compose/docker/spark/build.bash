#!/usr/bin/env bash
source ../bashfunctions.bash
set -e
export LH_VERSION=0.3.0

export SCALA_VERSION=2.12
export SPARK_MAJOR_VERSION=3.5
export SPARK_VERSION=3.5.6
export SPARK_NAME=spark-$SPARK_VERSION-bin-hadoop3
export SPARK_DISTR=$SPARK_NAME.tgz

export ICEBERG_VERSION=1.9.2
export ICE_JAR_NAME=iceberg-spark-runtime-"$SPARK_MAJOR_VERSION"_$SCALA_VERSION-$ICEBERG_VERSION.jar
export ICE_MAVEN=https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-"$SPARK_MAJOR_VERSION"_$SCALA_VERSION/$ICEBERG_VERSION


export APP_DIR=opt/lakehouse-task-spark-apps
export DRIVERS_DIR=drivers

##Prepare dir
mkdir -p $DRIVERS_DIR
mkdir -p $APP_DIR
downloadIfNotExists $ICE_JAR_NAME \
                    $ICE_MAVEN \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists $SPARK_DISTR \
                    https://dlcdn.apache.org/spark/spark-$SPARK_VERSION \
                    "." \
                    "http"
downloadIfNotExists awscli-exe-linux-x86_64.zip \
                     https://awscli.amazonaws.com \
                    "." \
                    "http"
downloadIfNotExists postgresql-42.7.8.jar \
                    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.8 \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists hadoop-aws-3.3.4.jar \
                    https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.3.4 \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists aws-java-sdk-bundle-1.12.262.jar \
                    https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.12.262 \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists wildfly-openssl-1.0.7.Final.jar \
                    https://repo1.maven.org/maven2/org/wildfly/openssl/wildfly-openssl/1.0.7.Final \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists lakehouse-task-executor-spark-dataset-app-0.3.0-jar-with-dependencies.jar \
                    ../../../lakehouse-task-executor-spark-dataset-app/target/ \
                    "$APP_DIR" \
                    "local"

downloadIfNotExists lakehouse-task-executor-spark-dq-app-0.3.0-jar-with-dependencies.jar \
                    ../../../lakehouse-task-executor-spark-dq-app/target/ \
                    "$APP_DIR" \
                    "local"


mkdir -p opt
rm -rf opt/$SPARK_NAME

tar xzvf  $SPARK_DISTR -C ./opt
export ENTRYPOINT_FILE=entrypoint.sh
unzip awscli-exe-linux-x86_64.zip -d ./opt/

echo "FROM eclipse-temurin:17-jdk-jammy" > Dockerfile
#echo "RUN apt-get update && apt-get install -y --no-install-recommends passwd && rm -rf /var/lib/apt/lists/*" >> Dockerfile
echo "ENV SPARK_VERSION=$SPARK_VERSION" >> Dockerfile
echo "ENV SPARK_NAME=spark-$SPARK_VERSION-bin-hadoop3" >> Dockerfile
echo "ENV SPARK_HOME=/opt/$SPARK_NAME" >> Dockerfile
echo "ENV SPARK_JARS=/opt/$SPARK_NAME/jars" >> Dockerfile
echo "ENV ENTRYPOINT_FILE=$ENTRYPOINT_FILE" >> Dockerfile
echo "ENV JAVA_OPTS='--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -XX:+IgnoreUnrecognizedVMOptions'" >> Dockerfile
echo "ADD ./opt /opt">> Dockerfile
echo "ADD ./drivers /opt/drivers">> Dockerfile
echo "ADD ./$ENTRYPOINT_FILE /">> Dockerfile
echo "RUN chmod +x $ENTRYPOINT_FILE">> Dockerfile
echo "ENV PATH=/opt/$SPARK_NAME/bin:$/opt/$SPARK_NAME/sbin:$PATH">> Dockerfile
echo "ENTRYPOINT /$ENTRYPOINT_FILE">> Dockerfile
echo "RUN /opt/aws/install && rm -rf /opt/aws">> Dockerfile
echo "RUN aws --version">> Dockerfile
echo "">> Dockerfile
echo "RUN cat /etc/group">> Dockerfile
echo "RUN echo 'spark:x:1000:' >> /etc/group && echo 'spark:x:1000:100:spark:/home/spark:/bin/sh' >> /etc/passwd &&  mkdir -p /home/spark && chown spark:users /home/spark ">> Dockerfile
echo "RUN chown -R spark:users /opt">> Dockerfile
echo "RUN chown -R spark:users /entrypoint.sh">> Dockerfile
echo "RUN chmod +rwxr+xr+xr ./$ENTRYPOINT_FILE">> Dockerfile
echo "USER spark">> Dockerfile
echo "">> Dockerfile

docker build -t lakehouse-spark:$LH_VERSION ./
docker images
rm -rf ./opt
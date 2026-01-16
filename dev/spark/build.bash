#!/usr/bin/env bash
set -e
export LH_VERSION=0.3.0

export SCALA_VERSION=2.12
export SPARK_MAJOR_VERSION=3.5
export SPARK_VERSION=3.5.7
export SPARK_NAME=spark-$SPARK_VERSION-bin-hadoop3
export SPARK_DISTR=$SPARK_NAME.tgz

export ICEBERG_VERSION=1.9.2
export ICE_JAR_NAME=iceberg-spark-runtime-"$SPARK_MAJOR_VERSION"_$SCALA_VERSION-$ICEBERG_VERSION.jar
export ICE_MAVEN=https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-"$SPARK_MAJOR_VERSION"_$SCALA_VERSION/$ICEBERG_VERSION


export APP_DIR=opt/lakehouse-task-spark-apps
export DRIVERS_DIR=opt/drivers

##Prepare dir
#rm -rf $APP_DIR
mkdir -p $APP_DIR
#rm -rf $DRIVERS_DIR
mkdir -p $DRIVERS_DIR


function downloadHTTP() {
    FILE_NAME=$1
    FILE_SOURCE=$2
    FILE_DEST=$3
    echo "Download from '$FILE_SOURCE'"
    wget -P $FILE_DEST "$FILE_SOURCE/$FILE_NAME"
}
function downloadLocal() {
    FILE_NAME=$1
    FILE_SOURCE=$2
    FILE_DEST=$3

    echo "Copy from '$FILE_SOURCE'"
    cp "$FILE_SOURCE/$FILE_NAME" $FILE_DEST
}

function downloadIfNotExists() {
  FILE_NAME=$1
  FILE_SOURCE=$2
  FILE_DEST=$3
  SOURCE_TYPE=$4
  if [ -f "$FILE_DEST/$FILE_NAME" ]; then
    echo "File '$FILE_DEST/$FILE_NAME' exists and is a regular file."
  else
    echo "File '$FILE_DEST/$FILE_NAME' does not exist or is not a regular file"

    if [[ "$SOURCE_TYPE" == "local" ]]; then
      downloadLocal $FILE_NAME $FILE_SOURCE $FILE_DEST
    elif [[ "$SOURCE_TYPE" == "http" ]]; then
      downloadHTTP $FILE_NAME $FILE_SOURCE $FILE_DEST
    else
      exit 1
    fi
  fi

}
downloadIfNotExists  lakehouse-task-spark-apps-$LH_VERSION-jar-with-dependencies.jar \
                      "../../lakehouse-task-spark-apps/target" \
                      "$APP_DIR" \
                      "local"

downloadIfNotExists $ICE_JAR_NAME \
                    $ICE_MAVEN \
                    "$DRIVERS_DIR" \
                    "http"
downloadIfNotExists $SPARK_DISTR \
                    https://dlcdn.apache.org/spark/spark-$SPARK_VERSION \
                    "." \
                    "http"
downloadIfNotExists postgresql-42.7.8.jar \
                    https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.8 \
                    "$DRIVERS_DIR" \
                    "http"
mkdir -p opt
rm -rf opt/$SPARK_NAME

tar xzvf  $SPARK_DISTR -C ./opt
export ENTRYPOINT_FILE=entrypoint.sh

echo "FROM eclipse-temurin:17" > Dockerfile
echo "ENV SPARK_VERSION=$SPARK_VERSION" >> Dockerfile
echo "ENV SPARK_NAME=spark-$SPARK_VERSION-bin-hadoop3" >> Dockerfile
echo "ENV SPARK_HOME=/opt/$SPARK_NAME" >> Dockerfile
echo "ENV SPARK_JARS=/opt/$SPARK_NAME/jars" >> Dockerfile
echo "ENV ENTRYPOINT_FILE=$ENTRYPOINT_FILE" >> Dockerfile
echo "ENV JAVA_OPTS='--add-exports=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.sql=ALL-UNNAMED --add-opens=java.sql/java.sql=ALL-UNNAMED --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/sun.security.action=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED -Djdk.reflect.useDirectMethodHandle=false -XX:+IgnoreUnrecognizedVMOptions'" >> Dockerfile
echo "ADD ./opt /opt">> Dockerfile
echo "ADD ./$ENTRYPOINT_FILE /">> Dockerfile
echo "RUN chmod +x $ENTRYPOINT_FILE">> Dockerfile
echo "ENV PATH=/opt/$SPARK_NAME/bin:$/opt/$SPARK_NAME/sbin:$PATH">> Dockerfile
echo "ENTRYPOINT /$ENTRYPOINT_FILE">> Dockerfile
echo "">> Dockerfile
echo "">> Dockerfile
echo "# todo useradd spark to own all spark processes" >> Dockerfile
echo "">> Dockerfile


docker build -t lakehouse-spark:$LH_VERSION ./
docker images
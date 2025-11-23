#!/usr/bin/env bash
set -e
export VERSION=0.3.0
export SPARK_VERSION=3.5.7
export SPARK_NAME=spark-$SPARK_VERSION-bin-hadoop3
export SPARK_DISTR=$SPARK_NAME.tgz

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
downloadIfNotExists  lakehouse-task-spark-apps-$VERSION-jar-with-dependencies.jar \
                      "../../lakehouse-task-spark-apps/target" \
                      "." \
                      "local"

downloadIfNotExists iceberg-spark-runtime-3.5_2.12-1.9.2.jar \
                    https://repo1.maven.org/maven2/org/apache/iceberg/iceberg-spark-runtime-3.5_2.12/1.9.2/$ICE_JAR_NAME \
                    "." \
                    "http"
downloadIfNotExists $SPARK_DISTR \
                    https://dlcdn.apache.org/spark/spark-$SPARK_VERSION \
                    "." \
                    "http"
rm -rf ./opt
rm -rf ./$SPARK_NAME
mkdir opt
tar xzvf  $SPARK_DISTR

docker build -t lakehouse-spark:$VERSION ./
docker images
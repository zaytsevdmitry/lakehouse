#!/usr/bin/env bash
set -e
export LH_VERSION=0.4.0
cp -f ../../lakehouse-task-executor-spark-dataset-app/target/lakehouse-task-executor-spark-dataset-app-0.4.0-jar-with-dependencies.jar ./
cp -f ../../lakehouse-task-executor-spark-dq-app/target/lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar ./
docker build -t lakehouse-spark-aws:$LH_VERSION ./
docker images
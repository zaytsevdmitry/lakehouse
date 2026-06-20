#!/usr/bin/env bash
set -e
set -v
cd  ../
pwd
mvn clean package
cd ./docker/lakehouse
bash ./build.bash
cd ../lakehouse-spark-aws
bash ./build.bash
cd ../hms
bash ./build.bash
cd ../checkcontainer
bash ./build.bash

docker images | grep lakehouse | grep '0.4.0'
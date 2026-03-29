#!/usr/bin/env bash
set -e
set -v
export VERSION=0.3.0
cd  ../
pwd
mvn clean package
cd ./demo/docker/lakehouse
bash ./build.bash
cd ../spark
bash ./build.bash
cd ../hms
bash ./build.bash

docker images | grep lakehouse
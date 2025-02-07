#!/usr/bin/env bash
cd ../
#mvn clean package
rm -rf distr
mkdir distr
mkdir ./distr/lakehouse-cli
mkdir ./distr/lakehouse-scheduler-svc
mkdir ./distr/lakehouse-config-svc

cp ./lakehouse-scheduler-svc/target/lakehouse-scheduler-svc-0.0.1-jar-with-dependencies.jar ./distr/lakehouse-scheduler-svc/
cp ./lakehouse-cli/target/lakehouse-cli-0.0.1-jar-with-dependencies.jar ./distr/lakehouse-cli/
cp ./lakehouse-config-svc/target/lakehouse-config-svc-0.0.1-jar-with-dependencies.jar ./distr/lakehouse-config-svc/

cp ./lakehouse-cli/src/main/resources/application.properties ./distr/lakehouse-cli/
cp ./lakehouse-config-svc/src/main/resources/application.yml ./distr/lakehouse-config-svc/
cp ./lakehouse-scheduler-svc/src/main/resources/application.yml ./distr/lakehouse-scheduler-svc/

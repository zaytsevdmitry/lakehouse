  #!/usr/bin/env bash
set -e
set -v
export VERSION=0.3.0
cd  ../
pwd
#mvn clean package
rm -rf distr
mkdir -p distr/opt

#mkdir ./distr/opt/lakehouse-cli
#mkdir ./distr/opt/lakehouse-scheduler-svc
#mkdir ./distr/opt/lakehouse-config-svc
#mkdir ./distr/opt/lakehouse-task-executor-svc
#mkdir ./distr/opt/lakehouse-state-svc
for app in "lakehouse-scheduler-svc" "lakehouse-cli" "lakehouse-task-spark-apps" "lakehouse-config-svc" "lakehouse-task-executor-svc" "lakehouse-state-svc"
do
  mkdir ./distr/opt/$app
  cp ./$app/target/$app-$VERSION-jar-with-dependencies.jar ./distr/opt/$app/
  cp ./$app/src/main/resources/application.yml ./distr/opt/$app/
done

#cp ./lakehouse-scheduler-svc/target/lakehouse-scheduler-svc-$VERSION-jar-with-dependencies.jar ./distr/opt/lakehouse-scheduler-svc/
#cp ./lakehouse-cli/target/lakehouse-cli-$VERSION-jar-with-dependencies.jar ./distr/opt/lakehouse-cli/
#cp ./lakehouse-config-svc/target/lakehouse-config-svc-$VERSION-jar-with-dependencies.jar ./distr/opt/lakehouse-config-svc/
#cp ./lakehouse-task-executor-svc/target/lakehouse-task-executor-svc-$VERSION.jar ./distr/opt/lakehouse-task-executor-svc/lakehouse-task-executor-svc-$VERSION-jar-with-dependencies.jar
#cp ./lakehouse-state-svc/target/lakehouse-state-svc-$VERSION-jar-with-dependencies.jar ./distr/opt/lakehouse-state-svc/

#cp ./lakehouse-cli/src/main/resources/application.properties ./distr/opt/lakehouse-cli/
#cp ./lakehouse-config-svc/src/main/resources/application.yml ./distr/opt/lakehouse-config-svc/
#cp ./lakehouse-scheduler-svc/src/main/resources/application.yml ./distr/opt/lakehouse-scheduler-svc/
#cp ./lakehouse-task-executor-svc/src/main/resources/application.yml  ./distr/opt/lakehouse-task-executor-svc/
#cp ./lakehouse-state-svc/src/main/resources/application.yml  ./distr/opt/lakehouse-state-svc/

cp demo/Dockerfile ./distr/

cd distr

docker build -t lakehouse ./
docker images
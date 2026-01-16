  #!/usr/bin/env bash
set -e
set -v
export VERSION=0.3.0
cd  ../
pwd
mvn clean package
rm -rf distr
mkdir -p distr/opt

for app in "lakehouse-scheduler-svc" "lakehouse-cli" "lakehouse-task-spark-apps" "lakehouse-config-svc" "lakehouse-task-executor-svc" "lakehouse-state-svc"
do
  echo Coping files to ./distr/opt/$app
  mkdir ./distr/opt/$app
  cp ./$app/target/$app-$VERSION-jar-with-dependencies.jar ./distr/opt/$app/
  cp ./$app/src/main/resources/application.yml ./distr/opt/$app/
done

cp demo/Dockerfile ./distr/

cd distr

docker build -t lakehouse ./
docker images
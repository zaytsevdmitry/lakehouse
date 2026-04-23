  #!/usr/bin/env bash
set -e
set -v
export LH_VERSION=0.3.0
pwd
#mvn clean package
mkdir -p ./opt
export CODE_ROOT="../../.."
for app in "lakehouse-scheduler-svc" "lakehouse-cli" "lakehouse-config-svc" "lakehouse-task-executor-svc" "lakehouse-state-svc"
do
  echo "Coping files $CODE_ROOT/$app/target/$app-$LH_VERSION-jar-with-dependencies.jar"
  cp -f $CODE_ROOT/$app/target/$app-$LH_VERSION-jar-with-dependencies.jar ./opt/
done

docker build -t lakehouse:$LH_VERSION ./
docker images
rm -rf ./opt
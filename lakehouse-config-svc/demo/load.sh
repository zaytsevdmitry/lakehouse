#!/usr/bin/env bash
export url=$1
pwd
ls ./
echo "server is 127.0.0.1:8080/v1_0/configs"

curl -i -X POST 127.0.0.1:8080/v1_0/configs/projects \
  -H "Content-Type: application/json" \
  --data-binary "@./projects/demo.json"


for s in "processingdb" "lakehousestorage"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/datastores \
     -H "Content-Type: application/json" \
     --data-binary "@./datastores/$s.json"
done

for s in "client_processing" "transaction_processing" "transaction_dds" "aggregation_pay_per_client_daily_mart" "aggregation_pay_per_client_total_mart"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/datasets \
     -H "Content-Type: application/json" \
     --data-binary "@./datasets/$s.json"
done

for s in "default"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/taskexecutionservicegroups \
     -H "Content-Type: application/json" \
     --data-binary "@./taskexecutionservicegroups/$s.json"
done

for s in "default"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/scenarios \
     -H "Content-Type: application/json" \
      --data-binary "@./scenario-act-templates/$s.json"
done


for s in "regular" "initial"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/schedules \
     -H "Content-Type: application/json" \
     --data-binary "@./schedules/$s.json"
done

curl -i -X GET 127.0.0.1:8080/v1_0/configs/effective_schedules/initial

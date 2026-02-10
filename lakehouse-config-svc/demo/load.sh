#!/usr/bin/env bash
set -e -x -v
export url=$1
pwd
ls ./
echo "server is 127.0.0.1:8080/v1_0/configs"

curl -i -X POST 127.0.0.1:8080/v1_0/configs/nameSpaces \
  -H "Content-Type: application/json" \
  --data-binary "@./name-spaces/demo.json"


for s in "postgres" "spark_iceberg"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/drivers \
     -H "Content-Type: application/json" \
     --data-binary "@./drivers/$s.json"
done

for s in "processingdb" "lakehousestorage"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/datasources \
     -H "Content-Type: application/json" \
     --data-binary "@./datasources/$s.json"
done


for s in "dataset-sql-model/client_processing" \
  "dataset-sql-model/transaction_processing" \
  "dataset-sql-model/transaction_dds" \
  "dataset-sql-model/aggregation_pay_per_client_daily_mart" \
  "dataset-sql-model/aggregation_pay_per_client_total_mart" \
  "dq/non_zero_count.sql" \
  "dq/non_zero_count_th.sql" \
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/scripts/"$s.sql" \
     -H "Content-Type: text/plain" \
     --data-binary "@./sql-scripts/$s.sql"
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

curl -i -X GET 127.0.0.1:8080/v1_0/configs/effective/schedules/name/initial

for s in "transaction_dds_qm"
do
   curl -i -X POST 127.0.0.1:8080/v1_0/configs/qualityMetrics \
     -H "Content-Type: application/json" \
     --data-binary "@./quality-metrics/$s.json"
done
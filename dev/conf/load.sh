#!/usr/bin/env bash
export url=$1
pwd
ls ./
echo "server is 127.0.0.1:8080/v1_0/configs"
echo "pwd is $PWD"
function curlPost() {
    URL=$1
    JSON_FILE=$2
    curl -f -i -X POST $URL -H "Content-Type: application/json" --show-error  --data-binary "@./$JSON_FILE"
    if [ $? -ne 0 ]; then
      echo "cURL error: $output"
      echo "Curl failed with an HTTP error. URL=$URL JSON_FILE=$JSON_FILE"
      exit 1
    fi
}
function curlGet() {
    URL=$1
    curl -f -i -X GET $URL --show-error
    if [ $? -ne 0 ]; then
      echo "Curl failed with an HTTP error. URL=$URL JSON_FILE=$JSON_FILE"
      exit 1
    fi
}


find ./sql-scripts/ -type f | while read -r f; do
    clean_name="${f#./sql-scripts/}" && clean_name="${clean_name//\//.}"
    curl -i -X POST 127.0.0.1:8080/v1_0/configs/scripts/"$clean_name" \
         -H "Content-Type: text/plain" \
         --data-binary "@$f"
done

curlPost 127.0.0.1:8080/v1_0/configs/nameSpaces "name-spaces/demo.json"

for s in "postgres" "spark_iceberg"
do
   curlPost 127.0.0.1:8080/v1_0/configs/drivers "drivers/$s.json"
done

for s in "processingdb" "lakehousestorage"
do
   curlPost 127.0.0.1:8080/v1_0/configs/datasources "datasources/$s.json"
done


for s in "client_processing" "transaction_processing" "transaction_dds" "aggregation_pay_per_client_daily_mart" "aggregation_pay_per_client_total_mart"
do
   file="dataset-sql-model/$s.sql"
   scriptName=`echo $file |sed 's/\//./g'`
   curlPost 127.0.0.1:8080/v1_0/configs/scripts/$scriptName "sql-scripts/$file"
done

for s in "non_zero_count" "non_zero_count_th"
do
   file="dq/$s.sql"
   scriptName=`echo $file |sed 's/\//./g'`
   curlPost 127.0.0.1:8080/v1_0/configs/scripts/$scriptName "sql-scripts/$file"
done

for s in "client_processing" "transaction_processing" "transaction_dds" "aggregation_pay_per_client_daily_mart" "aggregation_pay_per_client_total_mart"
do
   curlPost 127.0.0.1:8080/v1_0/configs/datasets "datasets/$s.json"
done

curl -f -i -X POST 127.0.0.1:8080/v1_0/configs/datasets -H "Content-Type: application/json" --data-binary "@./datasets/client_processing.json"
for s in "default"
do
   curlPost 127.0.0.1:8080/v1_0/configs/taskexecutionservicegroups "taskexecutionservicegroups/$s.json"
done

for s in "database" "spark"
do
   curlPost 127.0.0.1:8080/v1_0/configs/scenarios "scenario-act-templates/$s.json"
done


for s in "regular" "initial" "generateSourceDict" "generateSource"
do
   curlPost 127.0.0.1:8080/v1_0/configs/schedules "schedules/$s.json"
done

curlGet 127.0.0.1:8080/v1_0/configs/effective/schedules/name/initial
echo Quality metrics config
for s in "transaction_dds_qm" "transaction_dds_qm_const"
do
   curlPost 127.0.0.1:8080/v1_0/configs/quality/metrics "quality-metrics/$s.json"
done
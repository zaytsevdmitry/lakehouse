#!/usr/bin/env bash
export url=$1
pwd
ls ./
echo "server is 127.0.0.1:8080"

curl -i -X POST 127.0.0.1:8080/projects \
  -H "Content-Type: application/json" \
  --data-binary "@./projects-DEMO.json"


for s in "mydb" "someelsedb"
do
   curl -i -X POST 127.0.0.1:8080/datastores \
     -H "Content-Type: application/json" \
     --data-binary "@./datastores-$s.json"
done

for s in "anotherTable" "otherTable" "mytabledataSet"
do
   curl -i -X POST 127.0.0.1:8080/datasets \
     -H "Content-Type: application/json" \
     --data-binary "@./datasets-$s.json"
done

for s in "scenario1"
do
   curl -i -X POST 127.0.0.1:8080/scenarios \
     -H "Content-Type: application/json" \
      --data-binary "@./scenarios-$s.json"
done

for s in "default"
do
   curl -i -X POST 127.0.0.1:8080/taskexecutionservicegroups \
     -H "Content-Type: application/json" \
     --data-binary "@./taskexecutionservicegroups-$s.json"
done
for s in "regular" "initial"
do
   curl -i -X POST 127.0.0.1:8080/schedules \
     -H "Content-Type: application/json" \
     --data-binary "@./schedules-$s.json"
done

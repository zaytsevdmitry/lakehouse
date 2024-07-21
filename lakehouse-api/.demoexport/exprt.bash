rm -rf expdir
mkdir expdir

for s in "DEMO"
do
  curl -X GET  localhost:8080/projects/$s|jq > expdir/projects-$s.json
done

for s in "mytabledataSet" "anotherTable" "otherTable"
do
  curl -X GET  localhost:8080/datasets/$s|jq > expdir/datasets-$s.json
done

for s in "mydb" "someelsedb"
do
  curl -X GET  localhost:8080/datastores/$s|jq > expdir/datastores-$s.json
done

for s in "scenario1"
do
  curl -X GET  localhost:8080/scenarios/$s|jq > expdir/scenarios-$s.json
done

for s in "initial" "regular"
do
  curl -X GET  localhost:8080/schedules/$s|jq > expdir/schedules-$s.json
done

for s in "default"
do
  curl -X GET  localhost:8080/taskexecutionservicegroups/$s|jq > expdir/taskexecutionservicegroups-$s.json
done



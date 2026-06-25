rm -rf expdir

# "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     https://www.apache.org/licenses/LICENSE-2.0.txt
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


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



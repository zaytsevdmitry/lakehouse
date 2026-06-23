rm -rf expdir

# "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
# Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
# 
#     This program is free software: you can redistribute it and/or modify
#     it under the terms of the GNU Affero General Public License as
#     published by the Free Software Foundation, either version 3 of the
#     License, or (at your option) any later version.
# 
#     This program is distributed in the hope that it will be useful,
#     but WITHOUT ANY WARRANTY; without even the implied warranty of
#     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#     GNU Affero General Public License for more details.
# 
#     You should have received a copy of the GNU Affero General Public License
#     along with this program.  If not, see <https://www.gnu.org/licenses/>.

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



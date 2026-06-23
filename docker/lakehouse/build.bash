  #!/usr/bin/env bash

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

set -e
set -v
export LH_VERSION=0.5.0
pwd
mkdir -p ./opt
export CODE_ROOT="../.."
for app in "lakehouse-scheduler-svc" "lakehouse-cli" "lakehouse-config-svc" "lakehouse-task-executor-svc" "lakehouse-state-svc"
do
  echo "Coping files $CODE_ROOT/$app/target/$app-$LH_VERSION-jar-with-dependencies.jar"
  cp -f $CODE_ROOT/$app/target/$app-$LH_VERSION-jar-with-dependencies.jar ./opt/
done

docker build -t lakehouse:$LH_VERSION ./
docker images | grep lakehouse
rm -rf ./opt
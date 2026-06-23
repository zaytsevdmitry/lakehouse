# Stop tunnels

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

ps aux |grep kubectl |grep port-forward| grep  ' -n lakehouse-management'| awk '{print $2}'| xargs -r kill -9
# Stop sending tasks
kubectl scale deployment/lakehouse-management-task-executor-service --replicas=0 -n lakehouse-management
kubectl get pods -n lakehouse-management
kubectl get pod -n lakehouse-management -o name | grep 'exec-' |xargs -r kubectl -n lakehouse-management delete
kubectl get pod -n lakehouse-management -o name | grep '/task' | xargs -r kubectl delete -n lakehouse-management
helm uninstall lakehouse-release -n lakehouse-management
kubectl get pv|grep lakehouse-management |awk '{print $1}'| xargs kubectl delete pv
kubectl get pv | grep -E 'postgres|minio|lakehouse-management' | awk '{print $1}' | xargs -r kubectl delete pv
kubectl get crd | grep -E 'postgres|minio|acid'
kubectl delete namespace lakehouse-management
kubectl get pods -A
#остатки
minikube ssh "ls -la /tmp/hostpath-provisioner/ && sudo rm -rf /tmp/hostpath-provisioner/lakehouse-management && ls -la /tmp/hostpath-provisioner/"


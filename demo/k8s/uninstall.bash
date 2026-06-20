# Stop tunnels
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


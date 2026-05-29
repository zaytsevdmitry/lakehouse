helm uninstall lakehouse-release -n lakehouse-management
kubectl get pods -n lakehouse-management
kubectl get sparkapplication -n lakehouse-management -o name | grep '/regular' | xargs -r kubectl delete -n lakehouse-management
kubectl get pod -n lakehouse-management -o name | grep '/regular' | xargs -r kubectl delete -n lakehouse-management
kubectl get pv|grep lakehouse-management |awk '{print $1}'| xargs kubectl delete pv
kubectl get pv | grep -E 'postgres|minio|lakehouse-management' | awk '{print $1}' | xargs -r kubectl delete pv
kubectl get crd | grep -E 'postgres|minio|acid'
kubectl delete namespace lakehouse-management
kubectl get pods -A
#остатки
minikube ssh "ls -la /tmp/hostpath-provisioner/ && sudo rm -rf /tmp/hostpath-provisioner/lakehouse-management && ls -la /tmp/hostpath-provisioner/"

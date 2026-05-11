#!/usr/bin/env bash
minikube start --cpus 4 --memory 8192 --registry-mirror=https

#helm uninstall lakehouse-release -n lakehouse-management
helm dependency update ./lakehouse-management-helm-charts/lakehouse-management
minikube image load lakehouse:0.4.0
minikube image load postgres:latest
minikube image load apache/kafka:latest

helm install lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management --create-namespace  --namespace lakehouse-management


kubectl port-forward svc/lakehouse-management-config-service 8080:8080 -n lakehouse-management
kubectl port-forward svc/lakehouse-management-state-service  8081:8081 -n lakehouse-management
kubectl port-forward svc/db-dev 5432:5432 -n lakehouse-management
kubectl port-forward svc/broker 9092:9092 -n lakehouse-management
# helm upgrade lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management -n lakehouse-management
# kubectl scale deployment lakehouse-management-task-executor-service --replicas=3 -n lakehouse-management

helm repo add openbao https://openbao.github.io/openbao-helm
helm repo update
helm install openbao oci://ghcr.io/openbao/charts/openbao --namespace openbao --create-namespace
kubectl exec -ti openbao-0 -n openbao -- bao auth enable kubernetes


kubectl exec -ti openbao-0 -n openbao -- bao write auth/kubernetes/config \
    kubernetes_host="https://default.svc"
minikube image load lakehouse:0.4.0
minikube image pull postgres:latest
minikube image pull apache/kafka:latest

Hang tight while we grab the latest from your chart repositories...
...Successfully got an update from the "openbao" chart repository
Update Complete. ⎈Happy Helming!⎈
Update Complete. ⎈Happy Helming!⎈
Saving 1 charts
Deleting outdated charts
dm@localhost:~/IdeaProjects/lakehouse/dev/k8s>
I0501 22:32:09.349750  361228 warnings.go:110] "Warning: unknown field \"spec.initContainers\""
NAME: lakehouse-release
LAST DEPLOYED: Fri May  1 22:32:09 2026
NAMESPACE: lakehouse-management
STATUS: deployed
REVISION: 1
NOTES:
1. Get the application URL by running these commands:
  export POD_NAME=$(kubectl get pods --namespace lakehouse-management -l "app.kubernetes.io/name=lakehouse-management,app.kubernetes.io/instance=lakehouse-release" -o jsonpath="{.items[0].metadata.name}")
  export CONTAINER_PORT=$(kubectl get pod --namespace lakehouse-management $POD_NAME -o jsonpath="{.spec.containers[0].ports[0].containerPort}")
  echo "Visit http://127.0.0.1:8080 to use your application"
  kubectl --namespace lakehouse-management port-forward $POD_NAME 8080:$CONTAINER_PORT
dm@localhost:~/IdeaProjects/lakehouse/dev/k8s>


docker exec -it minikube bash

echo '{"exec-opts":["native.cgroupdriver=systemd"],"log-driver":"json-file","log-opts":{"max-size":"100m"},"storage-driver":"overlay2","registry-mirrors": ["https://dh-mirror.gitverse.ru"]}'>/etc/docker/daemon.json
root@minikube:/# service docker restart


minikube service lakehouse-management-config-service -n lakehouse-management
kubectl port-forward svc/lakehouse-management-config-service 8080:8080 -n lakehouse-management


kubectl --namespace lakehouse-management port-forward deployment/db-dev 5432:5432

#!/usr/bin/env bash

kubectl get pods -A

echo build helm dependencies
helm dependency update ./lakehouse-management-helm-charts/lakehouse-management

minikube image load lakehouse:0.4.0
minikube image load lakehouse-s3-check:0.4.0
minikube image load lakehouse-hms:0.4.0
minikube image load lakehouse-spark-aws:0.4.0



helm install lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management --create-namespace  --namespace lakehouse-management



xterm -e "kubectl port-forward svc/spark-history 18080:18080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-release-trino 9090:8080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/minio 9001:9001 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/minio 9000:9000 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-management-config-service 8080:8080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-management-state-service  8081:8081 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/db-dev 5432:5432 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/broker 9092:9092 -n lakehouse-management" &




kubectl get sparkapplication -n lakehouse-management -o name | grep '/regular' | xargs -r kubectl delete -n lakehouse-management
kubectl get pod -n lakehouse-management -o name | grep '/regular' | xargs -r kubectl delete -n lakehouse-management
kubectl get pods -n lakehouse-management

# uninstall.bash

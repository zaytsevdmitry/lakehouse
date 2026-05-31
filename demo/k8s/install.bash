#!/usr/bin/env bash
set -e
set -v
kubectl get pods -A

echo build helm dependencies
helm dependency update ./lakehouse-management-helm-charts/lakehouse-management

echo copy image lakehouse:0.4.0  ~ 2-3 minutes
minikube image load lakehouse:0.4.0 --daemon --alsologtostderr -v=1
echo copy image lakehouse:0.4.0  ~ 2-3 minutes
minikube image load lakehouse-s3-check:0.4.0 --daemon  --alsologtostderr -v=1
echo copy image lakehouse:0.4.0  ~ 2-3 minutes
minikube image load lakehouse-hms:0.4.0  --daemon --alsologtostderr -v=1
echo copy image lakehouse-spark-aws:0.4.0  ~ 2-3 minutes
minikube image load lakehouse-spark-aws:0.4.0 --daemon  --alsologtostderr -v=1
echo All lakehouse images loaded

echo install lakehouse-release
helm install lakehouse-release ./lakehouse-management-helm-charts/lakehouse-management --create-namespace  --namespace lakehouse-management


echo -e "\e[37;42m All services installed to namespace lakehouse-management \e[0m"
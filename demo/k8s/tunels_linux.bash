#!/usr/bin/env bash
xterm -e "kubectl port-forward svc/spark-history 18080:18080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-release-trino 9090:8080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/minio 9001:9001 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/minio 9000:9000 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-management-config-service 8080:8080 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/lakehouse-management-state-service  8081:8081 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/db-dev 5432:5432 -n lakehouse-management" &
xterm -e "kubectl port-forward svc/broker 9092:9092 -n lakehouse-management" &
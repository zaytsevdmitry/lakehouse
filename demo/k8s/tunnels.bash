#!/usr/bin/env bash
for svc in \
  "spark-history 18080:18080" \
  "lakehouse-release-trino 9090:8080" \
  "minio 9001:9001" \
  "minio 9000:9000" \
  "lakehouse-management-config-service 8080:8080" \
  "lakehouse-management-state-service 8081:8081" \
  "db-dev 5432:5432" \
  "broker 9092:9092"
do
  set -- $svc
  kubectl port-forward svc/$1 $2 -n lakehouse-management &
done
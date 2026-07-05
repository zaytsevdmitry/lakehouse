kubectl exec lakehouse-release-openbao-0 -n lakehouse-management -- /bin/sh -c '
  export BAO_TOKEN="root"

  echo "==> 1. Проверка движка секретов..."
  bao secrets enable -path=secret kv-v2 2>/dev/null || echo "KV уже включен"

  echo "==> 2. Запись секретов Базы Данных и S3..."
  bao kv put secret/lakehouse/processingdb user="postgresUser" password="postgresPW"
  bao kv put secret/lakehouse/s3 access_key="spark_user" secret_key="spark_pwd"

  echo "==> 3. Настройка аутентификации Kubernetes..."
  bao auth enable kubernetes 2>/dev/null || echo "Kubernetes auth уже включен"
  bao write auth/kubernetes/config kubernetes_host="https://default.svc"

  echo "==> 4. Создание политики чтения..."
  bao policy write lakehouse-read - <<EOF
path "secret/data/lakehouse/*" {
  capabilities = ["read"]
}
EOF

  echo "==> 5. Создание роли для Сервис-Аккаунта Spark..."
  bao write auth/kubernetes/role/spark-executor-role \
      bound_service_account_names=spark-driver-sa \
      bound_service_account_namespaces=lakehouse-management \
      policies=lakehouse-read \
      ttl=2h

  echo "==> НАСТРОЙКА УСПЕШНО ЗАВЕРШЕНА! <=="
'

- [checkcontainer](checkcontainer) - Легкий образ для запуска в k8s. Применяется для проверки доступности объектного хранилища

- [hms](hms) - Образ основанный на официальном apache/hive. Применяется для развертывания hive-metastore в k8s, compose. В образ добавлены библиотеки 
  - aws 
  - postgres

- [lakehouse](lakehouse) - Образ с сервисами управления данными 
 
  - lakehouse-scheduler-svc
  - lakehouse-cli
  - lakehouse-config-svc
  - lakehouse-task-executor-svc
  - lakehouse-state-svc 

- [lakehouse-spark-aws](lakehouse-spark-aws) - Образ основанный на официальном apache/spark. В образ добавлены библиотеки 
  - aws
  - postgres
  - [lakehouse-task-executor-spark-dataset-app](../lakehouse-task-executor-spark-dataset-app) с кодом обработки данных на spark
  - [lakehouse-task-executor-spark-dq-app](../lakehouse-task-executor-spark-dq-app) с кодом операций Data Quality на Spark
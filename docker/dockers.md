- [checkcontainer](checkcontainer) - Легкий образ для запуска в k8s. Применяется для проверки доступности объектного хранилища

- [hms](hms) - Образ основанный на официальном apache/hive. Применяется для развертывания hive-metastore в k8s, compose. В образ добавлены библиотеки 
  - aws 
  - postgres

- [jarina](jarina) - Образ с сервисами управления данными 
 
  - jarina-scheduler-svc
  - jarina-cli
  - jarina-config-svc
  - jarina-task-executor-svc
  - jarina-state-svc 

- [jarina-spark-aws](jarina-spark-aws) - Образ основанный на официальном apache/spark. В образ добавлены библиотеки 
  - aws
  - postgres
  - [jarina-task-executor-spark-dataset-app](../jarina-task-executor-spark-dataset-app) с кодом обработки данных на spark
  - [jarina-task-executor-spark-dq-app](../jarina-task-executor-spark-dq-app) с кодом операций Data Quality на Spark
  - [jarina-task-proxy4spark](jarina-task-proxy4spark) -  прокси сервис для запуска spark-job
    - jdk 23
  
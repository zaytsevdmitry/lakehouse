- [checkcontainer](checkcontainer) - Легкий образ для запуска в k8s. Применяется для проверки доступности объектного хранилища

- [hms](hms) - Образ основанный на официальном apache/hive. Применяется для развертывания hive-metastore в k8s, compose. В образ добавлены библиотеки 
  - aws 
  - postgres

- [yarina](yarina) - Образ с сервисами управления данными 
 
  - yarina-scheduler-svc
  - yarina-cli
  - yarina-config-svc
  - yarina-task-executor-svc
  - yarina-state-svc 

- [yarina-spark-aws](yarina-spark-aws) - Образ основанный на официальном apache/spark. В образ добавлены библиотеки 
  - aws
  - postgres
  - [yarina-task-executor-spark-dataset-app](../yarina-task-executor-spark-dataset-app) с кодом обработки данных на spark
  - [yarina-task-executor-spark-dq-app](../yarina-task-executor-spark-dq-app) с кодом операций Data Quality на Spark
  - [yarina-task-proxy4spark](yarina-task-proxy4spark) -  прокси сервис для запуска spark-job
    - jdk 23
  
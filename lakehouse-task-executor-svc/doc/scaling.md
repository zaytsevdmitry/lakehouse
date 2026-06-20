# Масштабирование task-executor-service
Как правило, все задачи выполняемые task-executor-service должны быть легковесными.
Основная задача сервиса - это разгрузка ответственности сервиса расписаний scheduler-service. task-executor-service является точкой распараллеливания выполнения задач. 
Он получает задание, адаптирует его для среды выполнения и передает ей, ожидая результата. Это не нагруженная, но блокирующая операция. 

## Важные параметры для управления масштабированием
в настройке экземпляра сервиса
- lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency
- lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id

```yaml
server:
  port: 8089
lakehouse:
  client:
    rest:
      state:
        server:
          url: http://127.0.0.1:8082
      config:
        server:
          url: http://127.0.0.1:8080
      scheduler:
        server:
          url: http://127.0.0.1:8081  
  task-executor:
    service:
      heart-beat-initial-delaY-ms: 5000
      heart-beat-interval-ms: 5000 
      max-lock-retries: 5
      max-lock-retries-duration-ms: 5
      id: first1  #<--- Имя , идентифицирующее экземпляр в составе группы. Каждому экземпляру нужно назначить свое мия
    scheduled:
      task:
        kafka:
          consumer:
            concurrency: 1 #<--- Количество потоков потребления
            properties:
              bootstrap.servers: 192.1.193.20:9092 #<--- брокер из которого получаем задания
              group.id: default #<--- Обычно используется для фиксации полученных offset. Сервис еще и фильтрует по этому значению, сообщения которые нужно выполнять
              auto.offset.reset: earliest 
            topics: scheduled_task_msg #<--- топик из которого получаем задания

```
в настройке задач
- taskExecutionServiceGroupName: default 

```json
{
      "name": "compact",
  
  --> "taskExecutionServiceGroupName": "default",

      "taskProcessor": "k8sSparkNativeTaskProcessor",
      "taskProcessorBody": "compactTableSQLProcessorBody",
      "importance": "critical",
      "description": "load from remote datastore",
      "taskProcessorArgs": {
        "spark.ui.enabled": "true",
        "spark.executor.memory": "1g",
        "spark.driver.memory": "1g",
        "datasource.service.protocol": "https",
        "lakehouse.client.rest.config.server.url": "http://lakehouse-management-config-service:8080",
        "k8s.spark-native.mainClass": "org.lakehouse.taskexecutor.spark.dataset.SparkProcessorApplication",
        "k8s.spark-native.appResource": "local:///opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dataset-app-0.4.0-jar-with-dependencies.jar"
      }
    }
```
> чтобы экземпляр сервиса взял задачу должны совпасть значения параметров 
 lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id
 и
 taskExecutionServiceGroupName
иначе задача будет проигнорирована, ожидается что она адресована другому task-executor-service

На пример

Берется для выполнения:
 - lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = default и taskExecutionServiceGroupName = default

Не берется для выполнения:
 - lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = black и taskExecutionServiceGroupName = white

## Вертикальное масштабирование (Scale Up)
Учитывая легковесную природу задач, первое, что можно сделать для повышения пропускной способности это увеличить количество потребителей задач
>lakehouse.task-executor.scheduled.task.kafka.consumer.properties=1

где 1 - значение заданное по умолчанию, означает работу в 1 потока потребления заданий. Увеличивая это число можно получить требуемую пропускную способность

> Несмотря на ожидаемую легковесность нужно понимать, что основными потребителями памяти являются *TaskProcessor, код реализованный в них, не должен раздувать память и допускать утечек. Нужно следить за расходом выделенной памяти, увеличивать его

## Горизонтальное масштабирование (Scale Out)
Достигается путем запуска дополнительных экземпляров сервиса. Это могут быть параллельно запущенные jvm на одмо или нескольких хостов. 
в случае k8s это могут быть реплики.

## Сегментирование (Segment-based Scaling).
Задачи разной природы могут занимать стабильно разное время блокировки. На пример:

|Группа процессоров | время работы  | Настройка                                                                                                                                                                                                             | Описание|                                                                                                                          | 
|-|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----|----------------------------------------------------------------------------------------------------|
|группа процессоров поддержки статусной модели датасетов| мили секунды | <br>lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency=1</br><br>lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = state</br><br>taskExecutionServiceGroupName = state</br>  | по своей сути мини rest клиент, который за мгновение получает или сообщает статус в сервис статусов и возвращается с результатом. 
| Spark - процессоры| десятки минут | <br>lakehouse.task-executor.scheduled.task.kafka.consumer.concurrency=20</br><br>lakehouse.task-executor.scheduled.task.kafka.consumer.properties.group.id = spark</br><br>taskExecutionServiceGroupName = spark</br> |Создается задача в кластере, что то читает и соединяет, потом записывает                                                                                                                         |
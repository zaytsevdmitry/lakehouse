#  Lakehouse management tool
Реализация подхода, основанного на метаданных (Metadata-Driven Approach), позволяет динамически управлять и автоматизировать ETL/ELT конвейеры и интеграцию данных. Отказ от жесткой, зашитой в код логики в пользу внешних конфигураций (SQL/JSON) превращает обработку данных в масштабируемую, гибкую и полностью автоматизированную экосистему.

Ключевые бизнес-выгоды
* Быстрее выход на рынок: вместо написания кода под каждый новый источник данных единый универсальный конвейер обрабатывает множество потоков. Это значительно сокращает циклы разработки и сроки проектов.
* Повышенная гибкость: бизнес-требования меняются быстро. При таком подходе изменения структур данных или правил трансформации выполняются простым изменением метаданных, без дорогостоящих и длительных переписываний кода.
* Улучшенное управление и соответствие требованиям: централизованные метаданные обеспечивают «единый источник истины» для происхождения данных (lineage), контроля доступа и регуляторных требований, гарантируя полную прозрачность.
* Экономически эффективная масштабируемость: разделяя логику обработки и трансформацию данных, организации могут эффективно управлять огромными объемами данных без пропорционального роста численности персонала.
* Интеллектуальная автоматизация: метаданные выступают в роли «двигателя», запускающего автоматизированные рабочие процессы и событийно-ориентированные процессы на основе реальных бизнес-потребностей.
* Улучшенная сопровождаемость: централизованные метаданные дают четкое представление о происхождении данных, контроле доступа и соответствии требованиям.
* Масштабируемость: разделение логики обработки и логики трансформации позволяет организациям эффективно управлять огромными объемами данных.
* Автоматизация: метаданные служат двигателем автоматических трансформаций и событийно-ориентированного исполнения.


# Ключевые слова
MetaData Driven
Domain Driven Design (DDD)
Data mesh
Data vault
Data governance
Scheduling
Custom code
SQL
Data engineer tool
Lakehouse management tool
United namespace


[Демо](demo/README.md)

# Статус проекта

| Компонент                                                                                | Статус      | Документация                                           |
|------------------------------------------------------------------------------------------|-------------|--------------------------------------------------------|
| [lakehouse-cli](lakehouse-cli)                                                           | Прототип    | [doc](lakehouse-cli/doc-ru/commandline.MD)             |
| [lakehouse-common-rest-client](lakehouse-common-rest-client)                             | Кандидат    |                                                        |
| [lakehouse-common-test](lakehouse-common-test)                                           | Кандидат    |                                                        |
| [lakehouse-config-rest-client](lakehouse-config-rest-client)                             | Кандидат    |                                                        |
| [lakehouse-config-svc](lakehouse-config-svc)                                             | Кандидат    | [doc](lakehouse-config-svc/doc-ru/readme.md)           |
| [lakehouse-scheduler-rest-client](lakehouse-scheduler-rest-client)                       | Кандидат    |                                                        |
| [lakehouse-scheduler-svc](lakehouse-scheduler-svc)                                       | Кандидат    | [doc](lakehouse-scheduler-svc/doc-ru/readme.md)        |
| [lakehouse-state-rest-client](lakehouse-state-rest-client)                               | Кандидат    |                                                        |
| [lakehouse-state-svc](lakehouse-state-svc)                                               | Кандидат    |                                                        |
| [lakehouse-task-executor-api](lakehouse-task-executor-api)                               | Кандидат    |                                                        |
| [lakehouse-task-executor-rest-client](lakehouse-task-executor-rest-client)               | Кандидат    |                                                        |
| [lakehouse-task-executor-svc](lakehouse-task-executor-svc)                               | Кандидат    | [doc](lakehouse-task-executor-svc/README_ru.md)        |
| [lakehouse-task-executor-spark-api](lakehouse-task-executor-spark-api)                   | Кандидат    | [doc](lakehouse-task-executor-spark-api/doc/readme.md) |
| [lakehouse-task-executor-spark-dataset-app](lakehouse-task-executor-spark-dataset-app)   | Кандидат    |                                                        |
| [lakehouse-task-executor-spark-dq-app](lakehouse-task-executor-spark-dq-app)             | Прототип    |                                                        |
| [lakehouse-validators](lakehouse-validators)                                             | Прототип    |                                                        |
| [lakehouse-ui-svc](lakehouse-ui-svc)                                                     | Прототип | [doc](lakehouse-ui-svc/doc-ru/readme.md)               |
| [lakehouse-task-proxy-for-spark](lakehouse-task-proxy-for-spark)                         | Кандидат    | [doc](lakehouse-task-proxy-for-spark/README_ru.md)     |
| [lakehouse-task-proxy-for-spark-api](lakehouse-task-proxy-for-spark-api)                 | Кандидат    |                                                        |
| Авторизация и безопасность                                                               | Не спроектирован |                                                        |
| [Docker](docker)                                                                         | Кандидат    | [doc](docker/readme.md)                                |



# Ссылки для разработчиков

[Системный дизайн](./doc-ru/system_design/system_design.md)

[Дизайн сущностей](./doc-ru/entities_design/entities_design.md)

[Планирование](lakehouse-scheduler-svc/doc-ru/scheduling/Scheduling.md)

[Состояния](lakehouse-state-svc/doc-ru/state_model/state-models.MD)

[Командная строка](./lakehouse-cli/doc-ru/commandline.MD)



# Авторские права
"Lakehouse management tool" - набор сервисов для управления изменениями данных на основе подхода, управляемого метаданными
    Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0.txt

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[EN](README.md)
#  Jarina dataset self service
Реализация подхода, основанного на метаданных (Metadata-Driven Approach), позволяет динамически управлять и автоматизировать ETL/ELT конвейеры и интеграцию данных. Отказ от жесткой, зашитой в код логики в пользу внешних конфигураций (SQL/JSON) превращает обработку данных в масштабируемую, гибкую и полностью автоматизированную экосистему.

# Для чего:
1. **Бизнес/продукт/data-аналитики (Analytics Engineers)** могут менять логику трансформации данных, просто поправив конфигурацию в UI, без переписывания кода ядра [0.11_ui].
2. **Бизнес-домены (Domain Teams)** могут владеть своими данными независимо друг от друга, не перегружая основную команду разработки [0.11_ui].
3. **Дата-инженеры (Data Engineers)** освобождаются от рутины (создания однотипных пайплайнов, ручных правок DDL) и фокусируются на сложных архитектурных и бизнес-задачах [0.11_ui].
4. **Владельцы продуктов (Product Owners)** экономят бюджет и время на разработке: фичи и аналитика запускаются быстрее (Time-to-Market) [0.11_ui], а дорогой ресурс дата-инженеров не тратится на однотипные задачи из бэклога.

# Ключевые бизнес-выгоды
* Быстрее выход на рынок(TTM): вместо написания кода под каждый новый источник данных единый универсальный конвейер обрабатывает множество потоков. Это значительно сокращает циклы разработки и сроки проектов.
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
Jarina dataset self service
United namespace


[Демо](demo/README.md)

# Статус проекта

| Компонент                                                                                | Статус   | Документация                                           |
|------------------------------------------------------------------------------------------|----------|--------------------------------------------------------|
| [jarina-cli](jarina-cli)                                                           | Прототип | [doc](jarina-cli/doc-ru/commandline.MD)             |
| [jarina-common-rest-client](jarina-common-rest-client)                             | Кандидат |                                                        |
| [jarina-common-test](jarina-common-test)                                           | Кандидат |                                                        |
| [jarina-config-rest-client](jarina-config-rest-client)                             | Кандидат |                                                        |
| [jarina-config-svc](jarina-config-svc)                                             | Кандидат | [doc](jarina-config-svc/doc-ru/readme.md)           |
| [jarina-scheduler-rest-client](jarina-scheduler-rest-client)                       | Кандидат |                                                        |
| [jarina-scheduler-svc](jarina-scheduler-svc)                                       | Кандидат | [doc](jarina-scheduler-svc/doc-ru/readme.md)        |
| [jarina-state-rest-client](jarina-state-rest-client)                               | Кандидат |                                                        |
| [jarina-state-svc](jarina-state-svc)                                               | Кандидат | [doc](jarina-state-svc/doc-ru/readme.md)            |
| [jarina-task-executor-api](jarina-task-executor-api)                               | Кандидат |                                                        |
| [jarina-task-executor-rest-client](jarina-task-executor-rest-client)               | Кандидат |                                                        |
| [jarina-task-executor-svc](jarina-task-executor-svc)                               | Кандидат | [doc](jarina-task-executor-svc/README_ru.md)        |
| [jarina-task-executor-spark-api](jarina-task-executor-spark-api)                   | Кандидат | [doc](jarina-task-executor-spark-api/doc/readme.md) |
| [jarina-task-executor-spark-dataset-app](jarina-task-executor-spark-dataset-app)   | Кандидат |                                                        |
| [jarina-task-executor-spark-dq-app](jarina-task-executor-spark-dq-app)             | Прототип |                                                        |
| [jarina-validators](jarina-validators)                                             | Прототип |                                                        |
| [jarina-ui-svc](jarina-ui-svc)                                                     | Прототип | [doc](jarina-ui-svc/doc-ru/readme.md)               |
| [jarina-task-proxy-for-spark](jarina-task-proxy-for-spark)                         | Кандидат | [doc](jarina-task-proxy-for-spark/README_ru.md)     |
| [jarina-task-proxy-for-spark-api](jarina-task-proxy-for-spark-api)                 | Кандидат |                                                        |
| Авторизация и безопасность                                                               | Кандидат | [doc](doc-ru/security/security.md)                     |
| [Docker](docker)                                                                         | Кандидат | [doc](docker/readme.md)                                |



# Ссылки для разработчиков

[Системный дизайн](./doc-ru/system_design/system_design.md)

[Дизайн сущностей](./doc-ru/entities_design/entities_design.md)

[Планирование](jarina-scheduler-svc/doc-ru/scheduling/Scheduling.md)

[Состояния](jarina-state-svc/doc-ru/state_model/state-models.MD)

[Командная строка](./jarina-cli/doc-ru/commandline.MD)



# Авторские права
"Jarina dataset self service" - набор сервисов для управления изменениями данных на основе подхода, управляемого метаданными
    Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        https://www.apache.org/licenses/LICENSE-2.0.txt

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

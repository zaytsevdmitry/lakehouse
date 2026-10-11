[RU](README_ru.md)
#  Jarina dataset self service
Implementing a Metadata-Driven Approach allows for the dynamic management and automation of ETL/ELT pipelines and data integration. By moving away from rigid, hard-coded logic to external configurations (SQL/JSON), we transform data processing into a scalable, flexible, and fully automated ecosystem.

## Target Audience & Purpose:

1. **Empower Analytics Engineers & Data Analysts:** To change data transformation logic seamlessly by simply updating configurations in the UI, completely bypassing the need to rewrite core pipeline code.
2. **Decentralized Domain Ownership:** To allow Domain Teams to own and manage their data independently, eliminating bottlenecks and offloading the core data engineering team.
3. **Free Data Engineers from Routine:** To automate repetitive tasks (such as manual DDL updates and building identical pipelines), allowing data engineers to focus on high-value business logic and scalable architecture.
4. **Optimize Product Management Costs (PO Value):** To help Product Owners save development time and budget. Feature delivery and data insights achieve a faster Time-to-Market, while expensive engineering resources are no longer wasted on repetitive backlog tickets.

## Key Business Benefits
* Faster Time-to-Market: Instead of writing custom code for every new data source, a single universal pipeline handles multiple streams. This significantly reduces development cycles and project timelines.
* Enhanced Agility: Business requirements change rapidly. With this approach, updates to data structures or transformation rules are handled by simply modifying metadata, avoiding costly and lengthy code rewrites.
* Improved Governance & Compliance: Centralized metadata provides a "single source of truth" for data lineage, access control, and regulatory requirements, ensuring total transparency.
* Cost-Effective Scalability: By decoupling processing logic from data transformation, organizations can manage massive data volumes efficiently without a proportional increase in headcount.
* Intelligent Automation: Metadata acts as the "engine" that triggers automated workflows and event-driven processes based on real-time business needs.
* Improved Maintainability: Centralized metadata provides clear visibility into data lineage, access control, and compliance.
* Scalability: Decoupling processing logic from transformation logic allows organizations to manage massive data volumes efficiently.
* Automation: Metadata serves as the engine for automated transformations and event-driven execution.


# Compatible keywords
MetaData Driven
Domain Driven Design (DDD)
Data mesh
Data Self Service
Data engineer tool
Data governance
Scheduling
Custom code
SQL
Jarina dataset self service
United namespace


[Demo](demo/README.md)

# Project status

| Component                                                                              | Status       | Documentation                                                                                   |   
|----------------------------------------------------------------------------------------|--------------|-------------------------------------------------------------------------------------------------|
| [jarina-cli](jarina-cli)                                                         | Prototype    | [doc](jarina-cli/doc/commandline.MD)                                                         |
| [jarina-common-rest-client](jarina-common-rest-client)                           | Candidate    |                                                                                                 |
| [jarina-common-test](jarina-common-test)                                         | Candidate    |                                                                                                 |
| [jarina-config-rest-client](jarina-config-rest-client)                           | Candidate    |                                                                                                 |
| [jarina-config-svc](jarina-config-svc)                                           | Candidate    | [doc](jarina-config-svc/doc/readme.md)                                                       |
| [jarina-scheduler-rest-client](jarina-scheduler-rest-client)                     | Candidate    |                                                                                                 |
| [jarina-scheduler-svc](jarina-scheduler-svc)                                     | Candidate    | [doc](jarina-scheduler-svc/doc/readme.md)                                                    |
| [jarina-state-rest-client](jarina-state-rest-client)                             | Candidate    |                                                                                                 |
| [jarina-state-svc](jarina-state-svc)                                             | Candidate    | [doc](jarina-state-svc/doc/readme.md)                                                        |
| [jarina-task-executor-api](jarina-task-executor-api)                             | Candidate    |                                                                                                 |
| [jarina-task-executor-rest-client](jarina-task-executor-rest-client)             | Candidate    |                                                                                                 |
| [jarina-task-executor-svc](jarina-task-executor-svc)                             | Candidate    | [doc](jarina-task-executor-svc/README.md)  |
| [jarina-task-executor-spark-api](jarina-task-executor-spark-api)                 | Candidate    | [doc](jarina-task-executor-spark-api/doc/readme.md)                                          |
| [jarina-task-executor-spark-dataset-app](jarina-task-executor-spark-dataset-app) | Candidate    |                                                                                                 |
| [jarina-task-executor-spark-dq-app](jarina-task-executor-spark-dq-app)           | Prototype    |                                                                                                 |
| [jarina-validators](jarina-validators)                                           | Prototype    |                                                                                                 |
| [jarina-ui-svc](jarina-ui-svc)                                                   | Prototype| [doc](jarina-ui-svc/doc/readme.md)                                                           |
| [jarina-task-proxy-for-spark](jarina-task-proxy-for-spark)                       | Candidate| [doc](jarina-task-proxy-for-spark/README.md)                                                 |
| [jarina-task-proxy-for-spark-api](jarina-task-proxy-for-spark-api)               | Candidate|                                                                                                 |
| Authorization & security                                                               | Candidate | [doc](doc/security/security.md)                                                                 |
| [Docker](docker)                                                                       | Candidate | [doc](docker/readme.md)                                                                         |



# Dev links

[System design](./doc/system_design/system_design.md)

[Entities design](./doc/entities_design/entities_design.md)

[Scheduling](jarina-scheduler-svc/doc/scheduling/Scheduling.md)

[States](jarina-state-svc/doc/state_model/state-models.MD)

[Command line](./jarina-cli/doc/commandline.MD)



# Copyright
"Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
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


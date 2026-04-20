#  Lakehouse management tool
Implementing a Metadata-Driven Approach allows for the dynamic management and automation of ETL/ELT pipelines and data integration. By moving away from rigid, hard-coded logic to external configurations (SQL/JSON), we transform data processing into a scalable, flexible, and fully automated ecosystem.
Key Business Benefits
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
Data vault
Data governance
Scheduling
Custom code
SQL
Data engineer tool
Lakehouse management tool
United namespace

# Configuration
[Configuration](lakehouse-config-svc/doc/configuration/configuration.md)


# Dev links

[System design](./doc/system_design/system_design.md)

[Entities design](./doc/entities_design/entities_design.md)

[Scheduling](lakehouse-scheduler-svc/doc/scheduling/Scheduling.md)

[States](lakehouse-state-svc/doc/state_model/state-models.MD)

[Command line](./lakehouse-cli/doc/commandline.MD)

[Demo](./demo/README.md)

# Project status

| Component                                                          | Status       | Documentation                             |   
|--------------------------------------------------------------------|--------------|-------------------------------------------|
| [lakehouse-cli](lakehouse-cli)                                     | Prototype    | [doc](lakehouse-cli/doc/commandline.MD)   |
| [lakehouse-common-rest-client](lakehouse-common-rest-client)       | Candidate    |
| [lakehouse-common-test](lakehouse-common-test)                     | Candidate    |
| [lakehouse-config-rest-client](lakehouse-config-rest-client)       | Candidate    |
| [lakehouse-config-svc](lakehouse-config-svc)                       | Candidate    | [doc](lakehouse-config-svc/doc/readme.md) |
| [lakehouse-scheduler-rest-client](lakehouse-scheduler-rest-client) | Candidate    |
| [lakehouse-scheduler-svc](lakehouse-scheduler-svc)                 | Candidate    | [doc](lakehouse-scheduler-svc/doc/readme.md)
| [lakehouse-state-rest-client](lakehouse-state-rest-client)         | Candidate    |
| [lakehouse-state-svc](lakehouse-state-svc)                         | Candidate    |
| [lakehouse-task-executor-api](lakehouse-task-executor-api)         | Candidate    |
| [lakehouse-task-executor-rest-client](lakehouse-task-executor-rest-client)         | Candidate    |
| [lakehouse-task-executor-svc](lakehouse-task-executor-svc)         | Candidate    | [doc](lakehouse-task-executor-svc/doc/devguide.md)
| [lakehouse-task-executor-spark-api](lakehouse-task-executor-spark-api)         | Candidate    |
| [lakehouse-task-executor-spark-dataset-app](lakehouse-task-executor-spark-dataset-app)      | Candidate    |
| [lakehouse-task-executor-spark-dq-app](lakehouse-task-executor-spark-dq-app)        | Prototype    |
| [lakehouse-validators](lakehouse-validators)      | Prototype    |
| [lakehouse-ui-svc](lakehouse-ui-svc)                               | Not designed |
| Authorization & security                                           | Not designed |

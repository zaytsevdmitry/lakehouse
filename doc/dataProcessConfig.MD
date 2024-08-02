# Compatible keywords
Domain Driven Design (DDD)
Data mesh
Data vault
Data governance
Scheduling
Custom code
SQL 
Data engineer tool
United namespace
# System design
![localImage](./services.png)
```plantuml
@startuml
!pragma layout smetana
database workshopDB

:ui-svc: as uisvc
:api-svc:  as apisvc
:task-coordinator-svc:  as taskcoordinatorsvc
:task-executor-svc:  as taskexecutorsvc
:task-scheduller-svc: as taskschedullersvc
:cvs-svc: as cvssvc
:cli: as cli
taskcoordinatorsvc -> apisvc
taskschedullersvc-->apisvc
cvssvc -->apisvc
uisvc-->apisvc
cli-->apisvc
apisvc-->workshopDB
taskexecutorsvc-->taskcoordinatorsvc:any one instance of task-executor take 1 task at onece
(sparkexecutors)<-->taskexecutorsvc:task-executorsvc is a spark  driver
@enduml
```
# Entities design
![localImage](./entities.png)
```plantuml
!pragma layout smetana

Project --|{ DataSet
DataStore --|{ DataSet
DataSet --|{ Schedule
ScenarioTemplate --|{ Schedule
ScenarioTemplate --|{ Task
DataSet }|-- DataSet
taskExecutionServiceGroup  --|{ Task
taskExecutionServiceGroup --|{ TaskExecutorService 
```

# Configuration files example
## system-repo
```

    DataStores
        |--mydb.yml
        |--someelsedb.yml
    projects
        |--mysecondproject.yml
        |--myfirstproject.yml     
    scenarios 
        |--default-scenario.yml
    taskExecutionServiceGroups
        |--UtilityGroup.yml
        |--QualityGroup.yml
        |--HeavyGroup.yml
        |--SuperHeavyGroup.yml
        |--HeavyTransformGroup.yml
        |--DeafaultGroup.yml
```  
## user-project-repo-myfirstproject
``` 

    datasets
      |--otherTable.yml
      |--anotherTable.yml
    schedules
       
``` 
## user-project-repo-mysecondproject
``` 
user-project-repo-mysecondproject
    datasets
      |--mytabledataSet.yml
    schedules
      |--mytableRegularSchedule.yaml
    scenarios 
      |--mynew-scenario.yml


```


# Project is a high level definition. Main namespace

```yaml
configtype: project
name: myfirstproject
comment: 
```
```yaml
configtype: project
name: mysecondproject
comment: 
```
# data stores - database, file/object storage or else some things
```yaml
configtype: DataStore
key: mydb
name:
interfaceType: jdbc
vendor: postgres
properties:
  url: jdbc:postgres:@localhost
  user: user1
  password: "****"
comment: 
```
```yaml
configtype: DataStore
key: someelsedb
interfaceType: file #http/jdbc/file etc
vendor: s3
properties:
  url: s3://somepath
  user: user1
  password: ${token}
```
#  DataSet declaration

```yaml
configtype: dataSet
key: otherTable
DataStoreKey: mydb
endPoint: mytabs.otherTable
projectKey: myfirstproject
```
```yaml
configtype: dataSet
key: anotherTable
catalogSchema: mytabs
DataStore: mydb
endPoint: mytabs.anotherTable
projectKey: myfirstproject
```
![localImage](./dependency.png)
```plantuml
!pragma layout smetana
(otherTable) --> (mytabledataSet)
(anotherTable) --> (mytabledataSet)
(myfirstproject)-->(mysecondproject)
(mydb)-->(mydb)
```
```yaml
configtype: dataSet
key: mytabledataSet
DataStore: mydb
endPoint: mytabs.mytable
project: mysecondproject
dependencies:
 - name: otherTable
 - name: anotherTable
description: "my first example mart"
columnSchema: 
  - name: id
    type: bigint
    nullable: true
    comment:  
  - name: tab_value
    type: string
    nullable: true
    comment:
keys:
  - name: mytable_pk
    type: primary
    runtimeLevelCheck: false
    constructLevelCheck: true
```
# increment query
```sql
select id, o.tab_value
  from otherTable o
  join anotherTable a
       using(id)
```

# ScenarioTemplate is a combination of dataSet, tasks and their execution dag
## 
## A single dataset configured as dataSet can be served by multiple scenarios.
```yaml
configtype: scenarioTemplate
scenarioType: dateEndPointScenarioTemplate

name: scenario1
    # private  endpoint = temporary tables 
internalEndPoints:
  - name: stageEndpoint
    DataStoreKey: mydb
    endPoint: mytabs.mytable_stage
  - name: mergeareaEndpoint
    DataStoreKey: mydb
    endPoint: mytabs.mytable_merge
tasks:
  - name: ${dataSet.name}_transformation
    sources: ${mytabledataSet.dependencies}
    endPoint: stageEndpoint
    executionModule: SQLTransformationDataFrameTask #inherited Task
    importance: critical # abort when error
    taskExecutionServiceGroup: HeavyTransformGroup
  - name: ${dataSet.name}_merge
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    executionModule: MergeDataFrameTask #inherited Task
    taskExecutionServiceGroup: HeavyGroup
    importance: critical
  - name: ${dataSet.name}_dataQualityCheckBefore
    sources: ${mergeareaEndpoint}
    endPoint: mergeareaEndpoint
    importance: critical
    taskExecutionServiceGroup: QualityGroup
    executionModule: DataQualityCheckTask #inherited Task
  - name: ${dataSet.name}_apply
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    importance: critical
    executionModule: ApplyTask #inherited Task
  - name: ${dataSet.name}_dataQualityCheckAfterCritical
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    executionModule: DataQualityCheckTask #inherited Task
    taskExecutionServiceGroup: QualityGroup
    importance: critical
  - name: ${dataSet.name}_dataQualityCheckAfterWarn
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    taskExecutionServiceGroup: UtilityGroup
    executionModule: DataQualityCheckTask #inherited Task
    importance: warn # pass when error
  - name: ${dataSet.name}_finally
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    taskExecutionServiceGroup: UtilityGroup
    executionModule: FinallyTask #inherited Task
    importance: warn # pass when error
dag:
  - ${dataSet.name}_transformation: ${dataSet.name}_merge
  - ${dataSet.name}_merge: ${dataSet.name}_dataQualityCheckBefore
  - ${dataSet.name}_dataQualityCheckBefore: ${dataSet.name}_apply
  - ${dataSet.name}_apply: ${dataSet.name}_dataQualityCheckAfterCritical
  - ${dataSet.name}_apply: ${dataSet.name}_dataQualityCheckAfterWarn
  - ${dataSet.name}_dataQualityCheckAfterCritical: ${dataSet.name}_finally
  - ${dataSet.name}_dataQualityCheckAfterWarn: ${dataSet.name}_finally

```
```
scenario1
         \ internalEndPoints
                             \ stageEndpoint
                             | mergearea
         \ task1 (take delta and save to stage)
         | task2 (quality check for delta)
         | task3 
         \ dagedges List<String,String> / name,name           
```
ScenarioTemplate DAG

![localImage](./taskpipline.png)

```plantuml
!pragma layout smetana
(transformation) --> (merge)
(merge) --> (dataQualityCheckBefore)
(dataQualityCheckBefore) --> (apply)
(apply) --> (dataQualityCheckAfterCritical)
(apply) --> (dataQualityCheckAfterWarn)
(dataQualityCheckAfterCritical) --> (finally)
(dataQualityCheckAfterWarn) --> (finally)
```

```
schedule1 --> scenarioRegular ------>---> dataSet
                                   /
schedule2 --> scenarioInintial ---/                
```
# Schedule
```yaml
configtype: schedule
name: mytableRegularSchedule
dataSet: mytabledataSet
scheduleExpr: "*8***"
scenarioTemplate: scenario1
dependencyIntervalType: Daily #montly/hourly/minutely/
keepDependencyInterval: true #false
```

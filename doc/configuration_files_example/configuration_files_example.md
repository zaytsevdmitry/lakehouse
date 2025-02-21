# Configuration files example
## Shared repository

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
 

## Team project repository named myfirstproject

```

    datasets
      |--otherTable.yml
      |--anotherTable.yml
    schedules
       
```
## Team project repository named mysecondproject

``` 

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
columnSchema:  # columnSchema will be sorted. 
  # The first time fileds wil be sorted  with  not empty order . 
  # Secondary added  other felleds sorted by name 
  - name: id
    type: bigint
    nullable: true
    comment:  #optional
    order:  #optional 
    sequence: true
  - name: tab_value
    type: string
    nullable: true
    comment:
keys:
  - name: mytable_pk
    type: primary
    columns: id
    enabled: true,
    runtimeLevelCheck: false
    constructLevelCheck: true
script: "increment.sql" 
```
*columnSchema*  will be sorted.
 The first time fields wil be sorted  with  not empty order.
 Secondary added  other fields sorted by name

# increment query
```sql
select id, o.tab_value
  from otherTable o
  join anotherTable a
       using(id)
```

# ScenarioTemplate
is a combination of dataSet, tasks and their execution dag
## 
## A one dataset configured as dataSet can be served by multiple scenarios.
```yaml
configtype: scenarioActTemplate
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

![localImage](./taskpipeline.png)

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
scenarioActTemplate: scenario1
dependencyIntervalType: Daily #montly/hourly/minutely/
keepDependencyInterval: true #false
```
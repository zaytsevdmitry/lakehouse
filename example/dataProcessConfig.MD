# Project - hight level definition

```yaml
configtype: project
name:myfirstproject
comment: 
```
```yaml
configtype: project
name:mysecondproject
```
# data stores - database, file/object storage or else some things
```

configtype: dataStoreEndpoints
  mydb
    interfaceType:jdbc
    vendor:postgres
    url:jdbc:postgres:@localhost
    user:user1
    password:****
  someelsedb
    interfaceType:file #http/jdbc/file etc
    vendor:s3
    url:s3//somepath
    user:user1
    password:${token}
```
# public endpoints = user table

```yaml
configtype: dataEndPoint
name: otherTable
dataStoreEndpointKey: mydb
endPoint: mytabs.otherTable
project: myfirstproject
```
```yaml
configtype: dataEndPoint
name: anotherTable
catalogSchema: mytabs
dataStoreEndpointKey: mydb
endPoint: mytabs.anotherTable
project: myfirstproject
```

```plantuml
!pragma layout smetana
(otherTable) --> (mytableDataEndPoint)
(anotherTable) --> (mytableDataEndPoint)
(myfirstproject)-->(mysecondproject)
(mydb)-->(mydb)
```
```yaml
configtype: dataEndPoint
name: mytableDataEndPoint
dataStoreEndpointKey: mydb
endPoint: mytabs.mytable
project: mysecondproject
dependencies:
 - name: otherTable
 - name: anotherTable
```
# A single dataset configured as dataEndPoint can be served by multiple scenarios.
```yaml
configtype: scenario
dataEndPoint: mytableDataEndPoint
name: scenario1
    # private  endpoint = temporary tables 
internalEndPoints:
  - name: stageEndpoint
    dataStoreEndpointKey: mydb
    endPoint: mytabs.mytable_stage
  - name: mergeareaEndpoint
    dataStoreEndpointKey: mydb
    endPoint: mytabs.mytable_merge
scheduleExpr: "*8***"
tasks:
  - name: ${dataEndPoint.name}_transformation
    sources: ${mytableDataEndPoint.dependencies}
    endPoint: stageEndpoint
    class: SQLTransformationDataFrameTask #inherited Task
    importance: critical # abort when error
  - name: ${dataEndPoint.name}_merge
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    class: MergeDataFrameTask #inherited Task
    importance: critical
  - name: ${dataEndPoint.name}_dataQualityCheckBefore
    sources: ${mergeareaEndpoint}
    endPoint: mergeareaEndpoint
    importance: critical
    class: MergeDataFrameTask #inherited Task
  - name: ${dataEndPoint.name}_apply
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    importance: critical
    class: MergeDataFrameTask #inherited Task
  - name: ${dataEndPoint.name}_dataQualityCheckAfterCritical
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    class: MergeDataFrameTask #inherited Task
    importance: critical
  - name: ${dataEndPoint.name}_dataQualityCheckAfterWarn
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    class: MergeDataFrameTask #inherited Task
    importance: warn # pass when error
  - name: ${dataEndPoint.name}_finally
    sources: ${stageEndpoint}
    endPoint: mergeareaEndpoint
    class: MergeDataFrameTask #inherited Task
    importance: warn # pass when error
dag:
  - ${dataEndPoint.name}_transformation: ${dataEndPoint.name}_merge
  - ${dataEndPoint.name}_merge: ${dataEndPoint.name}_dataQualityCheckBefore
  - ${dataEndPoint.name}_dataQualityCheckBefore: ${dataEndPoint.name}_apply
  - ${dataEndPoint.name}_apply: ${dataEndPoint.name}_dataQualityCheckAfterCritical
  - ${dataEndPoint.name}_apply: ${dataEndPoint.name}_dataQualityCheckAfterWarn
  - ${dataEndPoint.name}_dataQualityCheckAfterCritical: ${dataEndPoint.name}_finally
  - ${dataEndPoint.name}_dataQualityCheckAfterWarn: ${dataEndPoint.name}_finally



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
Scenario DAG

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
scenario1 ---
              \
               |-- dataEndPoint
              /
scenario2 ---                
```





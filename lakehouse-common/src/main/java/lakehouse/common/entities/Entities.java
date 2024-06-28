package lakehouse.common.entities;

import lakehouse.common.entities.task.Task;

import java.util.List;
import java.util.Properties;

/**
 * Definition of dataset
 */


record Project(int projectId, String projectName){}
record DataStore (String dataStoreId, Properties properties, String dataStoreAdapterId ){}
record DataEndPoint (int dataEndPointId, String dataEndPointName, DataStore dataStore, Project project ){}
record DagEdge(Task from, Task to){}
record Scenario(DataEndPoint dataEndPoint, List<Task> taskList, List<DagEdge> dagEdges){}
record TaskExecutorGroup(String taskExecutorGroupName){}
record Schedule(String scheduleName, String scheduleExpr,List<Scenario> scenarios,TaskExecutorGroup taskExecutorGroup){}
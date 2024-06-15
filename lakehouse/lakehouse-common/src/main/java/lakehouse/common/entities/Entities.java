package lakehouse.common.entities;

import lakehouse.common.entities.task.Task;

import java.util.List;
import java.util.Properties;

/**
 * Definition of dataset
 */


record Project(int projectId, String projectName){}
record DataEndPoint (int dataEndPointId, String dataEndPointName, String dataStoreId, Project project ){}
record DataStore (String dataStoreId, Properties properties, String dataStoreAdapterId ){}

record Scenario(DataEndPoint dataEndPoint, List<Task> taskList ){}
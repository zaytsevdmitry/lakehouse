package org.lakehouse.client.commandline.component.objectactionfacade.factory;

import org.lakehouse.client.commandline.component.objectactionfacade.*;
import org.lakehouse.client.exception.UnknownObjectTypeInCommandCombination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class ObjectActionsFactory {
	
	
	private final Map<String, ObjectActions> mapObjectActions;
	
	public ObjectActionsFactory(
			ProjectObjectActions projectObjectActions, 
			DataStoreObjectActions dataStoreObjectActions,
			DataSetObjectActions dataSetObjectActions,
			ScenarioActTemplateObjectActions scenarioActTemplateObjectActions,
			ScheduleObjectActions scheduleObjectActions,
			TaskExecutionServiceGroupObjectActions taskExecutionServiceGroupObjectActions,
			ScheduledTaskObjectActions scheduledTaskObjectActions,
			TaskLockObjectActionsImpl taskLockObjectActions) {
		Map<String, ObjectActions> mapObjectActions = new HashMap<String, ObjectActions>();
		mapObjectActions.put("project", projectObjectActions);
		mapObjectActions.put("datastore", dataStoreObjectActions);
		mapObjectActions.put("dataset", dataSetObjectActions);
		mapObjectActions.put("scenarioactemplate", scenarioActTemplateObjectActions);
		mapObjectActions.put("schedule", scheduleObjectActions);
		mapObjectActions.put("taskexecutionservicegroup", taskExecutionServiceGroupObjectActions);
		mapObjectActions.put("scheduledtask", scheduledTaskObjectActions);
		mapObjectActions.put("tasklock", taskLockObjectActions);
		
		
		this.mapObjectActions = new HashMap<String, ObjectActions>();
		this.mapObjectActions.putAll(mapObjectActions);

		mapObjectActions.forEach( (k,v) -> this.mapObjectActions.put(k+"s",(ObjectActions) v));
	}

	public ObjectActions getObjectActionsByObjectName(String name) throws UnknownObjectTypeInCommandCombination {
		
		if (mapObjectActions.containsKey(name.toLowerCase()))
			return mapObjectActions.get(name.toLowerCase());
		else throw new UnknownObjectTypeInCommandCombination();
	}

}

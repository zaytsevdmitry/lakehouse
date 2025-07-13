package org.lakehouse.client.commandline.component.objectactionfacade.factory;

import org.lakehouse.client.commandline.component.objectactionfacade.*;
import org.lakehouse.client.exception.UnknownObjectTypeInCommandCombination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class ConfigObjectActionsFactory {
	
	
	private final Map<String, ConfigObjectActions> mapObjectActions;
	
	public ConfigObjectActionsFactory(
			ProjectObjectActions projectObjectActions, 
			DataStoreObjectActions dataStoreObjectActions,
			DataSetObjectActions dataSetObjectActions,
			ScenarioActTemplateObjectActions scenarioActTemplateObjectActions,
			ScheduleObjectActions scheduleObjectActions,
			TaskExecutionServiceGroupObjectActions taskExecutionServiceGroupObjectActions
			) {
		Map<String, ConfigObjectActions> mapObjectActions = new HashMap<String, ConfigObjectActions>();
		mapObjectActions.put("project", projectObjectActions);
		mapObjectActions.put("datastore", dataStoreObjectActions);
		mapObjectActions.put("dataset", dataSetObjectActions);
		mapObjectActions.put("scenarioactemplate", scenarioActTemplateObjectActions);
		mapObjectActions.put("schedule", scheduleObjectActions);
		mapObjectActions.put("taskexecutionservicegroup", taskExecutionServiceGroupObjectActions);//mapObjectActions.put("scheduledtask", scheduledTaskObjectActions);
		
		
		this.mapObjectActions = new HashMap<String, ConfigObjectActions>();
		this.mapObjectActions.putAll(mapObjectActions);

		mapObjectActions.forEach( (k,v) -> this.mapObjectActions.put(k+"s",(ConfigObjectActions) v));
	}

	public ConfigObjectActions getObjectActionsByObjectName(String name) throws UnknownObjectTypeInCommandCombination {
		
		if (mapObjectActions.containsKey(name.toLowerCase()))
			return mapObjectActions.get(name.toLowerCase());
		else throw new UnknownObjectTypeInCommandCombination();
	}

}


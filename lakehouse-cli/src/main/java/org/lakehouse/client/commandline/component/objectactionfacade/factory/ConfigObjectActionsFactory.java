package org.lakehouse.client.commandline.component.objectactionfacade.factory;

import java.util.HashMap;
import java.util.Map;

import org.lakehouse.client.commandline.component.objectactionfacade.ConfigObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.DataSetObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.DataStoreObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.ProjectObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.ScenarioActTemplateObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.ScheduleObjectActions;
import org.lakehouse.client.commandline.component.objectactionfacade.TaskExecutionServiceGroupObjectActions;
import org.lakehouse.client.exception.UnknownObjectTypeInCommandCombination;
import org.springframework.stereotype.Component;
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
			//ScheduledTaskObjectActions scheduledTaskObjectActions
			) {
		Map<String, ConfigObjectActions> mapObjectActions = new HashMap<String, ConfigObjectActions>();
		mapObjectActions.put("project", projectObjectActions);
		mapObjectActions.put("datastore", dataStoreObjectActions);
		mapObjectActions.put("dataset", dataSetObjectActions);
		mapObjectActions.put("scenarioactemplate", scenarioActTemplateObjectActions);
		mapObjectActions.put("schedule", scheduleObjectActions);
		mapObjectActions.put("taskexecutionservicegroup", taskExecutionServiceGroupObjectActions);
		//mapObjectActions.put("scheduledtask", scheduledTaskObjectActions);
		
		
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


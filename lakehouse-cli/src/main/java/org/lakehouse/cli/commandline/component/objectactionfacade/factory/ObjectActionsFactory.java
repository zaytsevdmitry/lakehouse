package org.lakehouse.cli.commandline.component.objectactionfacade.factory;

import java.util.HashMap;
import java.util.Map;

import org.lakehouse.cli.commandline.component.objectactionfacade.DataSetObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.DataStoreObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.ObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.ProjectObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.ScenarioActTemplateObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.ScheduleObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.ScheduledTaskObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.TaskExecutionServiceGroupObjectActions;
import org.lakehouse.cli.commandline.component.objectactionfacade.TaskLockObjectActionsImpl;
import org.lakehouse.cli.exception.UnknownObjectTypeInCommandCombination;
import org.springframework.stereotype.Component;
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

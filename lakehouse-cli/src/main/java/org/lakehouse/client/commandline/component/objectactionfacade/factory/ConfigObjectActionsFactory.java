/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
            NameSpaceObjectActions nameSpaceObjectActions,
            DataStoreObjectActions dataStoreObjectActions,
            DataSetObjectActions dataSetObjectActions,
            ScenarioActTemplateObjectActions scenarioActTemplateObjectActions,
            ScheduleObjectActions scheduleObjectActions,
            TaskExecutionServiceGroupObjectActions taskExecutionServiceGroupObjectActions
    ) {
        Map<String, ConfigObjectActions> mapObjectActions = new HashMap<String, ConfigObjectActions>();
        mapObjectActions.put("nameSpace", nameSpaceObjectActions);
        mapObjectActions.put("datastore", dataStoreObjectActions);
        mapObjectActions.put("dataset", dataSetObjectActions);
        mapObjectActions.put("scenarioactemplate", scenarioActTemplateObjectActions);
        mapObjectActions.put("schedule", scheduleObjectActions);
        mapObjectActions.put("taskexecutionservicegroup", taskExecutionServiceGroupObjectActions);//mapObjectActions.put("scheduledtask", scheduledTaskObjectActions);


        this.mapObjectActions = new HashMap<String, ConfigObjectActions>();
        this.mapObjectActions.putAll(mapObjectActions);

        mapObjectActions.forEach((k, v) -> this.mapObjectActions.put(k + "s", (ConfigObjectActions) v));
    }

    public ConfigObjectActions getObjectActionsByObjectName(String name) throws UnknownObjectTypeInCommandCombination {

        if (mapObjectActions.containsKey(name.toLowerCase()))
            return mapObjectActions.get(name.toLowerCase());
        else throw new UnknownObjectTypeInCommandCombination();
    }

}


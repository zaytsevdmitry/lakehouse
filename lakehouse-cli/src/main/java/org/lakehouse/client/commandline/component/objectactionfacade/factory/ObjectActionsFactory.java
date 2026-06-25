/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
            NameSpaceObjectActions nameSpaceObjectActions,
            DataStoreObjectActions dataStoreObjectActions,
            DataSetObjectActions dataSetObjectActions,
            ScenarioActTemplateObjectActions scenarioActTemplateObjectActions,
            ScheduleObjectActions scheduleObjectActions,
            TaskExecutionServiceGroupObjectActions taskExecutionServiceGroupObjectActions,
            ScheduledTaskObjectActions scheduledTaskObjectActions,
            TaskLockObjectActionsImpl taskLockObjectActions) {
        Map<String, ObjectActions> mapObjectActions = new HashMap<String, ObjectActions>();
        mapObjectActions.put("nameSpace", nameSpaceObjectActions);
        mapObjectActions.put("datastore", dataStoreObjectActions);
        mapObjectActions.put("dataset", dataSetObjectActions);
        mapObjectActions.put("scenarioactemplate", scenarioActTemplateObjectActions);
        mapObjectActions.put("schedule", scheduleObjectActions);
        mapObjectActions.put("taskexecutionservicegroup", taskExecutionServiceGroupObjectActions);
        mapObjectActions.put("scheduledtask", scheduledTaskObjectActions);
        mapObjectActions.put("tasklock", taskLockObjectActions);


        this.mapObjectActions = new HashMap<String, ObjectActions>();
        this.mapObjectActions.putAll(mapObjectActions);

        mapObjectActions.forEach((k, v) -> this.mapObjectActions.put(k + "s", (ObjectActions) v));
    }

    public ObjectActions getObjectActionsByObjectName(String name) throws UnknownObjectTypeInCommandCombination {

        if (mapObjectActions.containsKey(name.toLowerCase()))
            return mapObjectActions.get(name.toLowerCase());
        else throw new UnknownObjectTypeInCommandCombination();
    }

}

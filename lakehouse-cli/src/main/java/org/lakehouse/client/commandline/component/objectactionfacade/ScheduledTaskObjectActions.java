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

package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduledTaskObjectActions implements ObjectActions {
    private final SchedulerRestClientApi schedulerRestClientApi;

    public ScheduledTaskObjectActions(SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
    }

    @Override
    public CommandResult showOne(String[] args) {
        return ObjectActionsHelper.getObjectJSONResult(schedulerRestClientApi.getScheduledTaskDTO(args[3]));
    }

    @Override
    public CommandResult showAll(String[] args) {
        List<ScheduledTaskDTO> l = schedulerRestClientApi.getScheduledTaskDTOList();

        return ObjectActionsHelper.table(
                new String[]{
                        "scheduleName",
                        "scheduleTargetDateTime",
                        "scenarioActName",
                        "name",
                        "status",
                        "executionModule",
                        "taskExecutionServiceGroupName"},
                l.stream().map(o -> new String[]{
                        o.getScheduleKeyName(),
                        o.getTargetDateTime(),
                        o.getScenarioActKeyName(),
                        o.getName(),
                        o.getStatus().label,
                        o.getTaskProcessor(),
                        o.getTaskExecutionServiceGroupName()
                }).toList());
    }

}

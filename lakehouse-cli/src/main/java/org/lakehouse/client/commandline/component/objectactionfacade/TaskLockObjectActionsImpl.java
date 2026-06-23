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

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TaskLockObjectActionsImpl implements TaskLockObjectActions {
    private final SchedulerRestClientApi schedulerRestClientApi;

    public TaskLockObjectActionsImpl(SchedulerRestClientApi schedulerRestClientApi) {
        this.schedulerRestClientApi = schedulerRestClientApi;
    }

    @Override
    public CommandResult showOne(String[] args) {
        return ObjectActionsHelper.getObjectJSONResult(schedulerRestClientApi.getScheduledTaskLockDTO(args[3]));
    }

    @Override
    public CommandResult showAll(String[] args) {
        List<ScheduledTaskLockDTO> l = schedulerRestClientApi.getScheduledTaskLockDTOList();

        return ObjectActionsHelper.table(
                new String[]{
                        "lockId",
                        "serviceId",
                        "lastHeartBeatDateTime"},
                l.stream().map(o -> new String[]{
                        o.getLockId().toString(),
                        o.getServiceId(),
                        o.getLastHeartBeatDateTime(),
                }).toList());
    }


    @Override
    public CommandResult lockNew(String[] args) {
        return ObjectActionsHelper.getObjectJSONResult(schedulerRestClientApi.lockTaskById(Long.valueOf(args[2]), args[3]));
    }

    @Override
    public CommandResult lockHeartBeat(String[] args) {
        TaskExecutionHeartBeatDTO h = new TaskExecutionHeartBeatDTO();
        h.setLockId(Long.valueOf(args[2]));
        return ObjectActionsHelper.coverHttpCode(schedulerRestClientApi.lockHeartBeat(h));

    }

    @Override
    public CommandResult lockRelease(String[] args) {
        TaskInstanceReleaseDTO r = new TaskInstanceReleaseDTO();
        r.setLockId(Long.valueOf(args[2].toUpperCase()));
        r.getTaskResult().setStatus(Status.Task.valueOf(args[3].toUpperCase()));
        int code = 0;
        code = schedulerRestClientApi.lockRelease(r);
        return ObjectActionsHelper.coverHttpCode(code);
    }
}

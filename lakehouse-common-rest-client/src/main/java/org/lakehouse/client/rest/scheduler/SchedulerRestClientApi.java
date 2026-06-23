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

package org.lakehouse.client.rest.scheduler;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;

import java.util.List;


public interface SchedulerRestClientApi {
    public ScheduledTaskDTO getScheduledTaskDTO(String name);

    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id);

    public int deleteScheduledTaskDTO(String name);

    public int postScheduledTaskDTO(ScheduledTaskMsgDTO o);

    public List<ScheduledTaskDTO> getScheduledTaskDTOList();

    public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOList();

    public ScheduledTaskLockDTO lockTaskById(Long taskId, String serviceId);

    public int lockHeartBeat(TaskExecutionHeartBeatDTO taskExecutionHeartBeat);

    public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO);
}
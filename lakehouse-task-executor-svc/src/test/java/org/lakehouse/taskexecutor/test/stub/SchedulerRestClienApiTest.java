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

package org.lakehouse.taskexecutor.test.stub;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;

import java.util.List;

public class SchedulerRestClienApiTest implements SchedulerRestClientApi {

    private final List<ScheduledTaskLockDTO> scheduledTaskLockDTOS;

    public SchedulerRestClienApiTest(List<ScheduledTaskLockDTO> scheduledTaskLockDTOS) {

        this.scheduledTaskLockDTOS = scheduledTaskLockDTOS;
    }

    @Override
    public ScheduledTaskDTO getScheduledTaskDTO(String name) {
        return null;
    }

    @Override
    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id) {
        return null;
    }

    @Override
    public int deleteScheduledTaskDTO(String name) {
        return 0;
    }

    @Override
    public int postScheduledTaskDTO(ScheduledTaskMsgDTO o) {
        return 0;
    }

    @Override
    public List<ScheduledTaskDTO> getScheduledTaskDTOList() {
        return List.of();
    }

    @Override
    public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOList() {
        return List.of();
    }

    @Override
    public ScheduledTaskLockDTO lockTaskById(Long taskId, String serviceId) {
        return null;
    }

    @Override
    public int lockHeartBeat(TaskExecutionHeartBeatDTO taskExecutionHeartBeat) {
        return 0;
    }

    @Override
    public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
        return 0;
    }
}

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

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.rest.RestClientHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;


public class SchedulerRestClientApiImpl implements SchedulerRestClientApi {

    private final RestClientHelper restClientHelper;

    public SchedulerRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public List<ScheduledTaskDTO> getScheduledTaskDTOList() {
        return Arrays.asList(Objects.requireNonNull(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.SCHEDULED_TASKS)
                .retrieve()
                .body(ScheduledTaskDTO[].class)));
    }

    @Override
    public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOList() {
        return Arrays.asList(Objects.requireNonNull(restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.SCHEDULED_TASKS_LOCKS)
                .retrieve()
                .body(ScheduledTaskLockDTO[].class)));
    }

    @Override
    public int postScheduledTaskDTO(ScheduledTaskMsgDTO o) {
        return restClientHelper.postDTO(o, Endpoint.SCHEDULED_TASKS_ID);
    }

    @Override
    public ScheduledTaskLockDTO lockTaskById(Long id, String serviceId) {
        return restClientHelper.getRestClient()
                .get()
                .uri(Endpoint.SCHEDULED_TASKS_LOCK_BY_ID, id, serviceId)
                .retrieve()
                .body(ScheduledTaskLockDTO.class);
    }

    @Override
    public int lockHeartBeat(TaskExecutionHeartBeatDTO taskExecutionHeartBeat) {
        return restClientHelper.putDTO(taskExecutionHeartBeat, Endpoint.SCHEDULED_TASKS_LOCK_HEARTBEAT);
    }

    @Override
    public int lockRelease(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
        return restClientHelper.putDTO(taskInstanceReleaseDTO, Endpoint.SCHEDULED_TASKS_RELEASE);
    }

    @Override
    public ScheduledTaskDTO getScheduledTaskDTO(String name) {
        return restClientHelper.getDtoOne(name, Endpoint.SCHEDULED_TASKS_ID, ScheduledTaskDTO.class);
    }

    @Override
    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id) {
        return restClientHelper.getDtoOne(id, Endpoint.SCHEDULED_TASKS_LOCK_ID, ScheduledTaskLockDTO.class);
    }

    @Override
    public int deleteScheduledTaskDTO(String name) {
        return restClientHelper.deleteDtoByName(name, Endpoint.SCHEDULED_TASKS_ID);
    }

}
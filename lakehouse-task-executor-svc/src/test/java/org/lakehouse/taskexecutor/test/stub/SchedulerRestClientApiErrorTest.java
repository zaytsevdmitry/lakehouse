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

package org.lakehouse.taskexecutor.test.stub;

import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;

import java.util.List;

public class SchedulerRestClientApiErrorTest implements SchedulerRestClientApi {
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

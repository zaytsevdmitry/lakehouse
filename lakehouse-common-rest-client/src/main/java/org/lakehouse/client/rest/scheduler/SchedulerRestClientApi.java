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
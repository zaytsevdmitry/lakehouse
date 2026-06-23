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

package org.lakehouse.taskexecutor.spark.dq.test;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.test.config.configuration.FileLoader;

import java.io.IOException;

public class TaskConfigTestFactory {


    public ScheduledTaskLockDTO loadScheduledTaskLockDTO(String dataSetKeyName, String taskName) throws IOException {

        ScheduledTaskLockDTO scheduledTaskLockDTO = new ScheduledTaskLockDTO();
        scheduledTaskLockDTO.setLockId(-1L);
        scheduledTaskLockDTO.setServiceId("test");
        scheduledTaskLockDTO.setLastHeartBeatDateTime(DateTimeUtils.nowStr());
        scheduledTaskLockDTO.setScheduledTaskEffectiveDTO(findScheduledTaskDTO(dataSetKeyName,taskName));
        return scheduledTaskLockDTO;
    }
    public static String targetDateTime = "2025-12-20T00:00:00Z";
    public static String intervalStart = targetDateTime;
    public static String intervalEnd = "2025-12-21T00:00:00Z";
    private ScheduledTaskDTO findScheduledTaskDTO(String dataSetKeyName, String taskName) throws IOException {
        FileLoader fileLoader = new FileLoader();
        ScheduledTaskDTO result = new ScheduledTaskDTO();

        TaskDTO t = fileLoader
                .loadScheduleEffectiveDTO()
                .getScenarioActs()
                .stream()
                .filter(a -> a.getDataSet().equals(dataSetKeyName))
                .flatMap(a-> a.getTasks().stream())
                .filter(taskDTO -> taskDTO.getName().equals(taskName))
                .toList().get(0);
        result.setDataSetKeyName(dataSetKeyName);
        result.setScheduleKeyName("unknown");
        result.setTaskProcessor(t.getTaskProcessor());
        result.setTaskProcessorBody(t.getTaskProcessorBody());
        result.setTaskProcessorArgs(t.getTaskProcessorArgs());
        result.setTargetDateTime(targetDateTime);
        result.setIntervalStartDateTime(intervalStart);
        result.setIntervalEndDateTime(intervalEnd);
        result.setId(1L);
        result.setScenarioActKeyName("unknown");
        result.setStatus(Status.Task.RUNNING);
        result.setTaskExecutionServiceGroupName("unknown");
        return result;
    }
}

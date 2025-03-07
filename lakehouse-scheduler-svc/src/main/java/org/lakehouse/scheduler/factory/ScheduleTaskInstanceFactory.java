package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;

public class ScheduleTaskInstanceFactory {
    public static ScheduleTaskInstance mapToNewScheduleTaskInstance(
            TaskDTO taskDTO,
            ScheduleScenarioActInstance scheduleScenarioActInstance) {
        ScheduleTaskInstance result = new ScheduleTaskInstance();
        result.setName(taskDTO.getName());
        result.setScheduleScenarioActInstance(scheduleScenarioActInstance);
        result.setStatus(Status.Task.NEW.label);
        return result;
    }
}

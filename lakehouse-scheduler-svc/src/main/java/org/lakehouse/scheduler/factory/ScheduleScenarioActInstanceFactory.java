package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleScenarioActEffectiveDTO;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;

public class ScheduleScenarioActInstanceFactory {
    public static  ScheduleScenarioActInstance mapToScheduleScenarioActInstance(
            ScheduleScenarioActEffectiveDTO scheduleScenarioActEffectiveDTO,
            ScheduleInstance scheduleInstance) {
        ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
        result.setName(scheduleScenarioActEffectiveDTO.getName());
        result.setScheduleInstance(scheduleInstance);
        result.setConfDataSetKeyName(scheduleScenarioActEffectiveDTO.getDataSet());
        result.setStatus(Status.ScenarioAct.NEW.label);
        return result;
    }
}

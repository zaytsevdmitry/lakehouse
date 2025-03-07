package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;

public  class ScheduleInstanceLastBuildFactory {


    public static ScheduleInstanceLastBuild mapDTOToScheduleInstanceLastBuild(
            ScheduleInstanceLastBuild instanceLastBuild,
            ScheduleEffectiveDTO scheduleEffectiveDTO){

        ScheduleInstanceLastBuild result = instanceLastBuild;

        result.setConfigScheduleKeyName(scheduleEffectiveDTO.getName());
        result.setEnabled(scheduleEffectiveDTO.isEnabled());
        result.setLastChangeNumber(scheduleEffectiveDTO.getLastChangeNumber());
        result.setLastChangedDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(scheduleEffectiveDTO.getLastChangedDateTime()));
        result.setLastUpdateDateTime(DateTimeUtils.now());
        return result;
    }
}

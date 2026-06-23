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

package org.lakehouse.scheduler.factory;

import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;

public class ScheduleInstanceLastBuildFactory {


    public static ScheduleInstanceLastBuild mapDTOToScheduleInstanceLastBuild(
            ScheduleInstanceLastBuild instanceLastBuild,
            ScheduleEffectiveDTO scheduleEffectiveDTO) {

        ScheduleInstanceLastBuild result = instanceLastBuild;

        result.setConfigScheduleKeyName(scheduleEffectiveDTO.getKeyName());
        result.setEnabled(scheduleEffectiveDTO.isEnabled());
        result.setLastChangeNumber(scheduleEffectiveDTO.getLastChangeNumber());
        result.setLastChangedDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(scheduleEffectiveDTO.getLastChangedDateTime()));
        result.setLastUpdateDateTime(DateTimeUtils.now());
        return result;
    }
}

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

package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;



public class DataSetStateDTOFactory {
    public static DataSetStateDTO buildtDataSetStateDTO(
            Status.DataSet status, ScheduledTaskDTO scheduledTaskDTO) {
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(scheduledTaskDTO.getDataSetKeyName());
        result.setIntervalStartDateTime(scheduledTaskDTO.getIntervalStartDateTime());
        result.setIntervalEndDateTime(scheduledTaskDTO.getIntervalEndDateTime());
        result.setStatus(status);
        result.setLockSource(scheduledTaskDTO.buildLockSource());

        return result;
    }

}

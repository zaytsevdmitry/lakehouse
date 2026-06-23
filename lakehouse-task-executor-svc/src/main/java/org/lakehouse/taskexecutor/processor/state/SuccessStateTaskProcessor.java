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

package org.lakehouse.taskexecutor.processor.state;

import org.apache.http.HttpStatus;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.service.DataSetStateDTOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service(value = "successStateTaskProcessor")
public class SuccessStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SuccessStateTaskProcessor(
            StateRestClientApi stateRestClientApi) {
        super(stateRestClientApi);
    }


    @Override
    public void runTask(SourceConfDTO sourceConfDTO,
                        ScheduledTaskDTO scheduledTaskDTO,
                        JinJavaUtils jinJavaUtils) throws TaskFailedException {
        DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.SUCCESS, scheduledTaskDTO);
        logger.info("Send SUCCESS {}", dataSetStateDTO);
        int resultCode = getStateRestClientApi().setDataSetStateDTO(dataSetStateDTO);
        if (resultCode != HttpStatus.SC_OK) {
            throw new TaskFailedException(String.format("HttpStatus is %d", resultCode));
        }
    }
}

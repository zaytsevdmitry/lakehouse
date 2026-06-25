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

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

package org.lakehouse.client.rest.taskexecutor;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.rest.RestClientHelper;

public class TaskExecutorRestClientApiImpl implements TaskExecutorRestClientApi {
    private final RestClientHelper restClientHelper;

    public TaskExecutorRestClientApiImpl(RestClientHelper restClientHelper) {
        this.restClientHelper = restClientHelper;
    }

    @Override
    public ScheduledTaskLockDTO getScheduledTaskLockDTO(Long lockId) {
        return restClientHelper.getDtoOne(lockId.toString(), Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID, ScheduledTaskLockDTO.class);
    }
}

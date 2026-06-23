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

package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.commandline.model.CommandResult;

public interface TaskLockObjectActions extends ObjectActions {
    /**
     * lock new <objectType> <taskExecutorGroup> <taskExecutorServiceId>
     * word          			index in array
     * ------------------------+----
     * lock          		   | 0
     * new                     | 1
     * taskId                  | 2
     * <taskExecutorServiceId> | 3
     */

    CommandResult lockNew(String[] args);

    /**
     * lock heartbeat <lockId>
     * word          			index in array
     * ------------------------+----
     * lock          		   | 0
     * heartbeat     		   | 1
     * <lockId>                | 2
     */

    CommandResult lockHeartBeat(String[] args);


    /**
     * lock release <lockId> <status>
     * word          			index in array
     * ------------------------+----
     * lock          		   | 0
     * release       		   | 1
     * <lockId>                | 2
     * <status>                | 3
     */

    CommandResult lockRelease(String[] args);
}

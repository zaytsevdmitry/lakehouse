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

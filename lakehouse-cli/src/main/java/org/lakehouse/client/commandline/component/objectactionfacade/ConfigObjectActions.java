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

public interface ConfigObjectActions extends ObjectActions {
    /**
     * upload <objectType> <filePath>
     * word          index in array
     * --------------+-------------
     * upload        | 0
     * <objectType>  | 1
     * <filePath>    | 2
     */

    CommandResult upload(String[] args) throws Exception;

    /**
     * download <objectType> <name|id> <filePath>
     * <p>
     * word          index in array
     * --------------+-------------
     * download      | 0
     * <objectType>  | 1
     * <name|id>     | 2
     * <filePath>    | 3
     */
    // todo move exceptions to CommandResult
    CommandResult download(String[] args);

    /**
     * delete <objectType> <name|id>
     * <p>
     * word          index in array
     * --------------+-------------
     * download      | 0
     * <objectType>  | 1
     * <name|id>     | 2
     */
    CommandResult delete(String[] args);
}

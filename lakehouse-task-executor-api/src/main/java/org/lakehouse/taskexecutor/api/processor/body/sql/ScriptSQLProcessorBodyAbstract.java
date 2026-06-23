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

package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class ScriptSQLProcessorBodyAbstract extends SQLProcessorBodyAbstract {

    public ScriptSQLProcessorBodyAbstract(
            ConfigRestClientApi configRestClientApi,
            DataSourceManipulatorFactory  dataSourceManipulatorFactory) {
        super(configRestClientApi,dataSourceManipulatorFactory);

    }

    public void execute(
            ExecuteUtils executeUtils,
            String template,
            String model) throws TaskFailedException {
        List<String> commands = Arrays.asList(model.split(SystemVarKeys.SCRIPT_DELIMITER));
        String mainSQL = commands.get(commands.size() - 1);
        List<String> preSQLs = new ArrayList<>();
        if (commands.size() > 1) {
            for (int i = 0; i < commands.size() - 1; i++) {
                preSQLs.add(commands.get(i));
            }
        }
        try {
            for (String preSQL : preSQLs) {
                executeUtils.execute(preSQL);
            }
            Map<String, Object> localContext = Map.of(SystemVarKeys.SCRIPT, mainSQL);
            executeUtils.execute(template, localContext);

        } catch (ExecuteException e) {
            throw new TaskFailedException(e);
        }
    }

}

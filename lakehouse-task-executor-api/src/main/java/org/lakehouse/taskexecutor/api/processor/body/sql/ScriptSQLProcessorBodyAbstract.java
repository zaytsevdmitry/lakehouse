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

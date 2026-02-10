package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class ScriptSQLProcessorBodyAbstract extends SQLProcessorBodyAbstract {

    public ScriptSQLProcessorBodyAbstract() {

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

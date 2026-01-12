package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class ScriptSQLProcessorBodyAbstract extends SQLProcessorBodyAbstract {

    public ScriptSQLProcessorBodyAbstract(BodyParam bodyParam) {
        super(bodyParam);
    }

    @Override
    public void run() throws TaskFailedException {
        List<String> commands = Arrays.asList(fullScript().split(SystemVarKeys.SCRIPT_DELIMITER));
        String mainSQL = commands.get(commands.size() - 1);
        List<String> preSQLs = new ArrayList<>();
        if (commands.size() > 1) {
            for (int i = 0; i < commands.size() - 1; i++) {
                preSQLs.add(commands.get(i));
            }
        }
        try {
            for (String preSQL : preSQLs) {
                targetDataSourceManipulator().executeUtils().execute(preSQL);
            }
            Map<String, Object> localContext = Map.of(SystemVarKeys.SCRIPT, mainSQL);
            targetDataSourceManipulator().executeUtils().execute(getModelTemplate(), localContext);

        } catch (ExecuteException e) {
            throw new TaskFailedException(e);
        }
    }

    protected abstract String getModelTemplate();
}

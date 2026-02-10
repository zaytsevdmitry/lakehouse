package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.springframework.stereotype.Service;

@Service(value = "appendSQLProcessorBody")
public class AppendSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    private final ConfigRestClientApi configRestClientApi;

    public AppendSQLProcessorBody(ConfigRestClientApi configRestClientApi) {

        this.configRestClientApi = configRestClientApi;
    }
    @Override
    public void run(BodyParam bodyParam) throws TaskFailedException {
        execute(
                bodyParam.targetDataSourceManipulator().executeUtils(),
                bodyParam.targetDataSourceManipulator().sqlTemplateDTO().getInsertDML(),
                configRestClientApi.getDataSetModelScript(bodyParam.targetDataSourceManipulator().dataSetDTO().getKeyName()));
    }
}

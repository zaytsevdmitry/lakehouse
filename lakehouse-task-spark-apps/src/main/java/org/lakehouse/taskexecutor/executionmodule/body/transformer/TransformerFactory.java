package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public class TransformerFactory {

    public DataTransformer buildDataTransformer(String script){

        return new SQLDataTransformer(script,
                //todo move delimiter to property
                "^/$");
    }
}

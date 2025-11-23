package org.lakehouse.taskexecutor.executionmodule.body.transformer;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public class TransformerFactory {

    public DataTransformer buildDataTransformer(TaskProcessorConfigDTO taskProcessorConfigDTO){

        // todo delimiter
        return new SQLDataTransformer(taskProcessorConfigDTO.getScripts().get(0), "^/$");
    }
}

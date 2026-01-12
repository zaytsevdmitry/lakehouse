package org.lakehouse.taskexecutor.executionmodule.body.transformer;

public class TransformerFactory {

    public DataTransformer buildDataTransformer(String script){

        return new SQLDataTransformer(script,
                //todo move delimiter to property
                "^/$");
    }
}

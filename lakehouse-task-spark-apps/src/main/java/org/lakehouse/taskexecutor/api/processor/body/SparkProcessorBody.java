package org.lakehouse.taskexecutor.api.processor.body;

public interface SparkProcessorBody extends ProcessorBody, SparkBodyParam{
    SparkBodyParam getBodyParam();
}


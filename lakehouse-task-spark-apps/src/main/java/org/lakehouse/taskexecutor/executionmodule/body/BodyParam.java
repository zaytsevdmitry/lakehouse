package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;

public class BodyParam {
    private SparkSession sparkSession;
    private TaskProcessorConfigDTO taskProcessorConfigDTO;
    private String[] otherArgs;

    public BodyParam(SparkSession sparkSession, TaskProcessorConfigDTO taskProcessorConfigDTO, String[] otherArgs) {
        this.sparkSession = sparkSession;
        this.taskProcessorConfigDTO = taskProcessorConfigDTO;
        this.otherArgs = otherArgs;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public void setSparkSession(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    public TaskProcessorConfigDTO getTaskProcessorConfigDTO() {
        return taskProcessorConfigDTO;
    }

    public void setTaskProcessorConfigDTO(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        this.taskProcessorConfigDTO = taskProcessorConfigDTO;
    }

    public String[] getOtherArgs() {
        return otherArgs;
    }

    public void setOtherArgs(String[] otherArgs) {
        this.otherArgs = otherArgs;
    }
}

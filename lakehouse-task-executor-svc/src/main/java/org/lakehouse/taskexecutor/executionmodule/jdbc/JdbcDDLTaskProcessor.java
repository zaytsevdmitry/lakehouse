package org.lakehouse.taskexecutor.executionmodule.jdbc;

import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.TableDialectFactory;
import org.lakehouse.client.api.factory.dialect.TableDialect;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;

public class JdbcDDLTaskProcessor extends JdbcAbstractTaskProcessor {
    public JdbcDDLTaskProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);

    }

    @Override
    public void runTask() throws TaskFailedException {
        TableDialect tableDialect = new TableDialectFactory().buildTableDialect(
                getTaskProcessorConfig().getTargetDataSourceDTO(),
                getTaskProcessorConfig().getTargetDataSet(),
                getTaskProcessorConfig().getForeignDataSetDTOMap()
        );

        tableDialect.getTableDDL();
    }
}

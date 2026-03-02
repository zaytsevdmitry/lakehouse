package org.lakehouse.taskexecutor.spark.dq.runner.integrity;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.Map;

public class CheckImpl implements Check{
    private final SQLTemplateDTO sqlTemplateDTO;
    private final SparkSession sparkSession;
    private final JinJavaUtils jinJavaUtils;
    public CheckImpl(
            SQLTemplateDTO sqlTemplateDTO,
            SparkSession sparkSession,
            JinJavaUtils jinJavaUtils) {
        this.sqlTemplateDTO = sqlTemplateDTO;
        this.sparkSession = sparkSession;
        this.jinJavaUtils = jinJavaUtils;
    }

    private Dataset<Row> execute(String sql, Map<String, Object> context){
        return sparkSession.sql(jinJavaUtils.render(sql,context));
    }
    @Override
    public Dataset<Row> nullableColumn(ColumnDTO columnDTO) {

        return execute(sqlTemplateDTO.getColumnNonNullCheckIntegrity(), Map.of(SystemVarKeys.COLUMN, columnDTO));
    }

    @Override
    public Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint) {
return null;
      //  return execute(sqlTemplateDTO.getColumnNonNullCheckIntegrity(), Map.of(SystemVarKeys.COLUMN, columnDTO));
    }

    @Override
    public Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint) {
        return null;
    }

    @Override
    public Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint) {
        return null;
    }

    @Override
    public Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint) {
        return null;
    }
}

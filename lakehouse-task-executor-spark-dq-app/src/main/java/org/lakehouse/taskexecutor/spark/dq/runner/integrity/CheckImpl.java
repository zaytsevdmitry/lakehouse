package org.lakehouse.taskexecutor.spark.dq.runner.integrity;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.HashMap;
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

    private Map<String,Object> getConstraintLocalContext(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        Map<String,Object> result = new HashMap<>();
        result.put(SystemVarKeys.CONSTRAINT_NAME, constraint.getKey());
        result.put(SystemVarKeys.CONSTRAINT, ObjectMapping.asMap(constraint.getValue()));
        return result;
    }
    @Override
    public Dataset<Row> getPrimary(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getPrimaryKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getForeign(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getForeignKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getUnique(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getUniqueKeyCheckIntegrity(), getConstraintLocalContext(constraint));
    }

    @Override
    public Dataset<Row> getCheck(Map.Entry<String, DataSetConstraintDTO> constraint) throws JsonProcessingException {
        return execute(sqlTemplateDTO.getCheckConstraintCheckIntegrity(), getConstraintLocalContext(constraint));
    }
}

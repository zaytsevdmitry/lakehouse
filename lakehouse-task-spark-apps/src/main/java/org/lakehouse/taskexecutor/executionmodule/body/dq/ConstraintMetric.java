package org.lakehouse.taskexecutor.executionmodule.body.dq;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;

import java.util.List;

public class ConstraintMetric implements Metric{

    private final SparkSession sparkSession;
    private final List<DataSetConstraintDTO> constraints;
    private final DataSetDTO dataSetDTO;
    public ConstraintMetric(
            SparkSession sparkSession,
            List<DataSetConstraintDTO> constraints, DataSetDTO dataSetDTO) {
        this.sparkSession = sparkSession;
        this.constraints = constraints;
        this.dataSetDTO = dataSetDTO;
    }

    @Override
    public Dataset<Row> calculate() throws TaskFailedException{
        Dataset<Row> result = SparkSession.active().emptyDataFrame();
        for (DataSetConstraintDTO constraint :constraints){
            Dataset<Row> current = null;
            Types.Constraint type = constraint.getType();
            if (type.equals(Types.Constraint.primary)) current = getPrimary(constraint);
            else if (type.equals(Types.Constraint.foreign)) current =   getForeign();
            else if (type.equals(Types.Constraint.unique)) current =   getUnique();
            else if (type.equals(Types.Constraint.check)) current =   getCheck();
            else throw new TaskFailedException(String.format( "Unexpected constraint type %s",type.label));
            result.unionAll(current);
        }
        return result;
    }

    public Dataset<Row> getPrimary(DataSetConstraintDTO constraint){



        String uniqueSql = """
                select ${metricName}, ${columns} , count(1) cnt
                from ${table}
                group by ${columns}
                having count(1) > 1
                """;
        if constraint.
        return SparkSession.active().emptyDataFrame();
    }
    public Dataset<Row> getForeign(){
        return SparkSession.active().emptyDataFrame();
    }
    public Dataset<Row> getUnique(){
        return SparkSession.active().emptyDataFrame();
    }
    public Dataset<Row> getCheck(){
        return SparkSession.active().emptyDataFrame();
    }
}

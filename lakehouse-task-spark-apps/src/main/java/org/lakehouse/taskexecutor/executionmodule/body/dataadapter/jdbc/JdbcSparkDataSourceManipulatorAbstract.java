package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.SparkDataSourceManipulatorAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public abstract class JdbcSparkDataSourceManipulatorAbstract extends SparkDataSourceManipulatorAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JdbcUtils jdbcUtils = new JdbcUtils();

    public JdbcSparkDataSourceManipulatorAbstract(DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
    }

    public JdbcUtils getJdbcUtils() {
        return jdbcUtils;
    }


    @Override
    public Dataset<Row>  read(Map<String, String> options) throws ReadException {
        Map<String, String> o = new HashMap<>();
        o.putAll(jdbcUtils.dtoToProps(getDataSourceDTO()));
        o.put("dbtable", getDataSetDTO().getFullTableName());
        o.putAll(options);
        return super.read(o);
    }
    @Override
    public void write(
            Dataset<Row> dataset,
            Map<String, String> options,
            Configuration.ModificationRule modificationRule) throws WriteException {
        try {
            dataset.writeTo(
                    getCatalogTableFullName())
                    .append();
        } catch (Exception e) {
            throw new WriteException(e);
        }
    }

    @Override
    public void truncate(String location, Map<String, String> options) throws TruncateException {
        String truncateSQL = String.format("TRUNCATE TABLE %s", location);
        try {
            jdbcUtils.execute(truncateSQL, options);
        } catch (SQLException e) {
            throw new TruncateException(e);
        }
    }
}

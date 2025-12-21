package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.factory.dialect.TableDialect;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;

import java.util.List;
import java.util.Map;

public interface
DataSourceManipulator extends DataSourceManipulatorParameter {


    Dataset<Row> read(Map<String, String> options)  throws ReadException;

    void createIfNotExists() throws CreateException;

    void write(Dataset<Row> dataset, Map<String, String> options, Configuration.ModificationRule modificationRule) throws WriteException;

    void drop() throws DropException;

    void truncate(String location, Map<String, String> options) throws TruncateException;

    void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException;

    void truncatePartitions(String location, List<String> partitions, Map<String, String> options) throws TruncateException;

    void exchangePartitions(List<String> partitions, String locationFrom, String locationTo, Map<String, String> options, Configuration.ModificationRule modificationRule);

    void removeConstraints() throws ConstraintException;

    void addConstraints() throws ConstraintException;

    void compact(Map<String, String> options) throws CompactException;

    void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException;

    Dataset<Row> executeQuery(String query, boolean enablePushDown);

    String getCatalogTableFullName();
}
package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.SparkDataSourceManipulatorAbstract;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.CompactException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ConstraintException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class FileSparkDataSourceManipulatorAbstract extends SparkDataSourceManipulatorAbstract {


    public FileSparkDataSourceManipulatorAbstract(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
    }

    @Override
    public void write(
            Dataset<Row> dataset,
            Map<String, String> options,
            Configuration.ModificationRule modificationRule) throws WriteException {
        try {
            dataset.write().mode(modificationRule.getValue()).format(getFormat()).save();
        } catch (Exception e) {
            throw new WriteException(e);
        }
    }

    private void deleteFSDirectory(String location, boolean isRecursively) throws IOException {
        FileSystem fs =  FileSystem.get(getSparkSession()
                        .sparkContext()
                        .hadoopConfiguration());
        Path filePath = new Path(location.concat("/"));
        fs.delete(filePath, isRecursively); // `false` for not recursively deleting a directory
    }
    @Override
    public void truncate(String location, Map<String, String> options) throws TruncateException {
        try {
            deleteFSDirectory(location,true);
        } catch (IOException e) {
            throw new TruncateException(e);
        }

    }

    @Override
    public void truncatePartitions(String location, List<String> partitions, Map<String, String> options) throws TruncateException {
        for (String partition:partitions){
            try {
                deleteFSDirectory(location.concat("/").concat(partition),true);
            } catch (IOException e) {
                throw new TruncateException(e);
            }
        }
    }

    @Override
    public void exchangePartitions(List<String> partitions, String locationFrom, String locationTo, Map<String, String> options, Configuration.ModificationRule modificationRule) {

    }

    @Override
    public void removeConstraints() throws ConstraintException {

    }

    @Override
    public void addConstraints() throws ConstraintException {

    }

    @Override
    public void compact(Map<String, String> options) throws CompactException {

    }

    @Override
    public void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException {

    }
}

package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;

import java.io.IOException;

public class FileSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {


    public FileSparkSQLDataSourceManipulator(
            SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        super(sparkSQLDataSourceManipulatorParameter);
    }

    private void deleteFSDirectory(String location, boolean isRecursively) throws IOException {
        FileSystem fs =  FileSystem.get(sparkSession()
                        .sparkContext()
                        .hadoopConfiguration());
        Path filePath = new Path(location.concat("/"));
        fs.delete(filePath, isRecursively); // `false` for not recursively deleting a directory
    }





}

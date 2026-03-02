package org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.spark;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.SparkSQLDataSourceManipulatorAbstract;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;

import java.io.IOException;

public  class FileSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {


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

/*

    @Override
    public void truncatePartitions(List<String> partitions) throws TruncateException {
        for (String partition:partitions){
            try {
                deleteFSDirectory(
                        jinjava().render(
                                "{{dataSets["+ SystemVarKeys.TARGET_DATASET_KEY_NAME +"]" +
                                        ".properties["+DataSetPropertyKeys.Key.LOCATION+"]}}"
                                        .concat("/").concat(partition),new HashMap<>()),
                        true);
            } catch (IOException e) {
                throw new TruncateException(e);
            }
        }
    }
*/


}

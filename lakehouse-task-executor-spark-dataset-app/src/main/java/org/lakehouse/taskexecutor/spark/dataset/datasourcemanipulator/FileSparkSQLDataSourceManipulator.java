/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

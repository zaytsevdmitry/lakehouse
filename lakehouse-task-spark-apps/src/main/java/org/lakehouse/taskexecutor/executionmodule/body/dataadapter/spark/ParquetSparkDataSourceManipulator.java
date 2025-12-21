package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;

import java.util.List;
import java.util.Map;

public  class ParquetSparkDataSourceManipulator extends FileSparkDataSourceManipulatorAbstract {

    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }
    public ParquetSparkDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
    }


    @Override
    public void createIfNotExists() {

    }

    @Override
    public void drop() throws DropException {

    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {

    }
}

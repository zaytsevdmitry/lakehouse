package org.lakehouse.taskexecutor.api.datasource;

import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.taskexecutor.api.datasource.exception.*;

import java.util.List;
import java.util.Map;

public interface DataSourceManipulator extends DataSourceManipulatorParameter {

    void addConstraints(Map<String,DataSetConstraintDTO> constraints) throws ConstraintException;
    void compact() throws CompactException;
    void compactPartitions(List<String> partitions) throws CompactException;
    void createTableIfNotExists() throws CreateException;
    void drop() throws DropException;
    void dropPartitions(List<String> partitions) throws DropException;
    void removeConstraintByName(String constraintName) throws ConstraintException;
    void removeConstraints(Map<String,DataSetConstraintDTO> constraints) throws ConstraintException;
    void truncate() throws TruncateException;
    void truncatePartitions(List<String> partitions) throws TruncateException;
}
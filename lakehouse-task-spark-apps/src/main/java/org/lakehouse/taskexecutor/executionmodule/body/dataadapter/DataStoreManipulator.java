package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;

import java.util.List;
import java.util.Map;

public interface DataStoreManipulator {
    public enum ModificationRule {
        write("w"),
        overwrite("o"),
        errorIfExists("e");
        private String value;

        private ModificationRule(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    //    Map<String,String> getDataStoreManipulatorConfiguration();
    Dataset<Row> read(String location, Map<String, String> options) throws ReadException;

    void write(Dataset<Row> dataset, String location, Map<String, String> options, ModificationRule modificationRule) throws WriteException;

    void truncate(String location, Map<String, String> options) throws TruncateException;

    void truncatePartitions(String location, List<String> partitions, Map<String, String> options) throws TruncateException;

    void exchangePartitions(List<String> partitions, String locationFrom, String locationTo, Map<String, String> options, ModificationRule modificationRule);

    void removeConstraints() throws ConstraintException;

    void addConstraints() throws ConstraintException;

    void compact(String location, Map<String, String> options) throws CompactException;

    void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException;
}

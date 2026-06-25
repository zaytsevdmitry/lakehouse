/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
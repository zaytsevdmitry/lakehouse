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
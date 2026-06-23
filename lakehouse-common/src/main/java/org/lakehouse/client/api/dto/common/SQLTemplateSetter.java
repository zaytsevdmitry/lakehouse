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

package org.lakehouse.client.api.dto.common;

public interface SQLTemplateSetter {
void setDatabaseSchemaName(String databaseSchemaName);

void setDatabaseSchemaDDLCreate(String databaseSchemaDDLCreate);

void setDatabaseSchemaExistsSQL(String databaseSchemaExistsSQL);

void setTableFullName(String tableFullName);

void setTableDDLCreate(String tableDDLCreate);

void setTableSQLExists(String tableSQLExists);

void setTableDDLDrop(String tableDDLDrop);

void setTableDDLTruncate(String tableDDLTruncate);

void setTableDDLCompact(String tableDDLCompact);

void setColumnNonNullCheckIntegrity(String columnNonNullCheckIntegrity);

void setColumnCheckIntegrity(String columnCheckIntegrity);

void setPartitionDDLExchange(String partitionDDLExchange);

void setPartitionDDLDrop(String partitionDDLDrop);

void setPartitionDDLTruncate(String partitionDDLTruncate);

void setPartitionDDLAdd(String partitionDDLAdd);

void setPartitionDDLCompact(String partitionDDLCompact);

void setConstraintDDLDrop(String constraintDDLDrop);

void setPrimaryKeyDDL(String primaryKeyDDL);

void setPrimaryKeyDDLAdd(String primaryKeyDDLAdd);

void setPrimaryKeyCheckIntegrity(String primaryKeyCheckIntegrity);

void setForeignKeyDDL(String foreignKeyDDL);

void setForeignKeyDDLAdd(String foreignKeyDDLAdd);

void setForeignKeyCheckIntegrity(String foreignKeyCheckIntegrity);

void setUniqueKeyDDL(String uniqueKeyDDL);

void setUniqueKeyDDLAdd(String uniqueKeyDDLAdd);

void setUniqueKeyCheckIntegrity(String uniqueKeyCheckIntegrity);

void setCheckConstraintDDL(String checkConstraintDDL);

void setCheckConstraintDDLAdd(String checkConstraintDDLAdd);

void setCheckConstraintCheckIntegrity(String checkConstraintCheckIntegrity);

void setColumnsCastDML(String columnsCastDML);

void setMergeDML(String mergeDML);

void setInsertDML(String insertDML);
}

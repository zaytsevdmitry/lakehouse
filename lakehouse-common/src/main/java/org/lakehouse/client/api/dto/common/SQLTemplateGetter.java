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

public interface SQLTemplateGetter {
    String getDatabaseSchemaName();

    String getDatabaseSchemaDDLCreate();

    String getDatabaseSchemaExistsSQL();

    String getTableFullName();

    String getTableDDLCreate();

    String getTableSQLExists();

    String getTableDDLDrop();

    String getTableDDLTruncate();

    String getTableDDLCompact();

    String getColumnNonNullCheckIntegrity();

    String getColumnCheckIntegrity();

    String getPartitionDDLExchange();

    String getPartitionDDLDrop();

    String getPartitionDDLTruncate();

    String getPartitionDDLAdd();

    String getPartitionDDLCompact();

    String getConstraintDDLDrop();

    String getPrimaryKeyDDL();

    String getPrimaryKeyDDLAdd();

    String getPrimaryKeyCheckIntegrity();

    String getForeignKeyDDL();

    String getForeignKeyDDLAdd();

    String getForeignKeyCheckIntegrity();

    String getUniqueKeyDDL();

    String getUniqueKeyDDLAdd();

    String getUniqueKeyCheckIntegrity();

    String getCheckConstraintDDL();

    String getCheckConstraintDDLAdd();

    String getCheckConstraintCheckIntegrity();

    String getColumnsCastDML();

    String getMergeDML();

    String getInsertDML();
}

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
package org.lakehouse.ui.dto;

public class ConstraintDTO {

    private String name;
    private String type;
    private String columns;
    private boolean enabled;
    private String constraintLevelCheck;
    private String checkExpr;
    private String tableConstraintDDLCreateOverride;
    private String tableConstraintDDLAddOverride;
    private String referencedTable;
    private String referenceConstraintName;
    private String onDelete;
    private String onUpdate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConstraintLevelCheck() {
        return constraintLevelCheck;
    }

    public void setConstraintLevelCheck(String constraintLevelCheck) {
        this.constraintLevelCheck = constraintLevelCheck;
    }

    public String getCheckExpr() {
        return checkExpr;
    }

    public void setCheckExpr(String checkExpr) {
        this.checkExpr = checkExpr;
    }

    public String getTableConstraintDDLCreateOverride() {
        return tableConstraintDDLCreateOverride;
    }

    public void setTableConstraintDDLCreateOverride(String tableConstraintDDLCreateOverride) {
        this.tableConstraintDDLCreateOverride = tableConstraintDDLCreateOverride;
    }

    public String getTableConstraintDDLAddOverride() {
        return tableConstraintDDLAddOverride;
    }

    public void setTableConstraintDDLAddOverride(String tableConstraintDDLAddOverride) {
        this.tableConstraintDDLAddOverride = tableConstraintDDLAddOverride;
    }

    public String getReferencedTable() {
        return referencedTable;
    }

    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    public String getReferenceConstraintName() {
        return referenceConstraintName;
    }

    public void setReferenceConstraintName(String referenceConstraintName) {
        this.referenceConstraintName = referenceConstraintName;
    }

    public String getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(String onDelete) {
        this.onDelete = onDelete;
    }

    public String getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(String onUpdate) {
        this.onUpdate = onUpdate;
    }
}

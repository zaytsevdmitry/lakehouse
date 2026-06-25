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

package org.lakehouse.client.api.dto.configs.dataset;

import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

public class DataSetConstraintDTO {
    private Types.Constraint type;
    private String columns = "";
    private boolean enabled = true;
    private Types.ConstraintLevelCheck constraintLevelCheck;
    private ForeignKeyReferenceDTO reference;
    private String checkExpr = "";
    private String tableConstraintDDLCreateOverride;
    private String tableConstraintDDLAddOverride;
    public DataSetConstraintDTO() {
    }

    public Types.Constraint getType() {
        return type;
    }

    public void setType(Types.Constraint type) {
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

    public String getTableConstraintDDLAddOverride() {
        return tableConstraintDDLAddOverride;
    }

    public void setTableConstraintDDLAddOverride(String tableConstraintDDLAddOverride) {
        this.tableConstraintDDLAddOverride = tableConstraintDDLAddOverride;
    }


    public String getTableConstraintDDLCreateOverride() {
        return tableConstraintDDLCreateOverride;
    }

    public void setTableConstraintDDLCreateOverride(String tableConstraintDDLCreateOverride) {
        this.tableConstraintDDLCreateOverride = tableConstraintDDLCreateOverride;
    }

    public ForeignKeyReferenceDTO getReference() {
        return reference;
    }

    public void setReference(ForeignKeyReferenceDTO reference) {
        this.reference = reference;
    }

    public Types.ConstraintLevelCheck getConstraintLevelCheck() {
        return constraintLevelCheck;
    }

    public void setConstraintLevelCheck(Types.ConstraintLevelCheck constraintLevelCheck) {
        this.constraintLevelCheck = constraintLevelCheck;
    }

    public String getCheckExpr() {
        return checkExpr;
    }

    public void setCheckExpr(String checkExpr) {
        this.checkExpr = checkExpr;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetConstraintDTO that = (DataSetConstraintDTO) o;
        return isEnabled() == that.isEnabled() &&
                getType() == that.getType() && Objects.equals(getColumns(), that.getColumns()) && getConstraintLevelCheck() == that.getConstraintLevelCheck() && Objects.equals(getReference(), that.getReference()) && Objects.equals(getTableConstraintDDLCreateOverride(), that.getTableConstraintDDLCreateOverride()) && Objects.equals(getTableConstraintDDLAddOverride(), that.getTableConstraintDDLAddOverride());
    }

    @Override
    public int hashCode() {
        return Objects.hash( getType(), getColumns(), isEnabled(), getConstraintLevelCheck(), getReference(), getTableConstraintDDLCreateOverride(), getTableConstraintDDLAddOverride());
    }

    @Override
    public String toString() {
        return "DataSetConstraintDTO{" +
                ", type=" + type +
                ", columns='" + columns + '\'' +
                ", enabled=" + enabled +
                ", constraintLevelCheck=" + constraintLevelCheck +
                ", reference=" + reference +
                ", tableConstraintDDLCreateOverride='" + tableConstraintDDLCreateOverride + '\'' +
                ", tableConstraintDDLAddOverride='" + tableConstraintDDLAddOverride + '\'' +
                '}';
    }
}

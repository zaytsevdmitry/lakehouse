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

package org.lakehouse.client.api.dto.configs.dataset;


import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

public class ForeignKeyReferenceDTO {
    private String dataSetKeyName;
    private String constraintName;
    private Types.ReferenceAction onDelete;
    private Types.ReferenceAction onUpdate;


    public ForeignKeyReferenceDTO() {
    }


    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public Types.ReferenceAction getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(Types.ReferenceAction onDelete) {
        this.onDelete = onDelete;
    }

    public Types.ReferenceAction getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(Types.ReferenceAction onUpdate) {
        this.onUpdate = onUpdate;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyReferenceDTO that = (ForeignKeyReferenceDTO) o;
        return Objects.equals(dataSetKeyName, that.dataSetKeyName) && Objects.equals(constraintName, that.constraintName) && Objects.equals(onDelete, that.onDelete) && Objects.equals(onUpdate, that.onUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetKeyName, constraintName, onDelete, onUpdate);
    }
}

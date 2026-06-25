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

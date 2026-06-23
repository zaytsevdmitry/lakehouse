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


import org.lakehouse.client.api.dto.configs.NameDescriptionAbstract;

import java.util.Objects;

public class ColumnDTO extends NameDescriptionAbstract {


    private String dataType;
    private boolean nullable;
    private Integer order = null;
    private boolean sequence;

    public ColumnDTO() {
    }


    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }

    public boolean isSequence() {
        return sequence;
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDTO columnDTO = (ColumnDTO) o;
        return isNullable() == columnDTO.isNullable() && isSequence() == columnDTO.isSequence() && Objects.equals(getDataType(), columnDTO.getDataType()) && Objects.equals(getOrder(), columnDTO.getOrder()) && Objects.equals(getDescription(), columnDTO.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), isNullable(), getOrder(), isSequence(), getDescription());
    }

    @Override
    public String toString() {
        return "\nColumnDTO{" +
                "\ndataType='" + dataType + '\'' +
                "\n, nullable=" + nullable +
                "\n, order=" + order +
                "\n, sequence=" + sequence +
                '}';
    }
}

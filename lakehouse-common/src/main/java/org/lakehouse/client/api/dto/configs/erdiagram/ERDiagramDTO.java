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

package org.lakehouse.client.api.dto.configs.erdiagram;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Visual diagram over {@code kind: DataSet} entities. The document only stores
 * which data sets are placed on the canvas together with their coordinates;
 * relationships between the data sets are derived from their constraints.
 */
public class ERDiagramDTO implements Serializable {

    private String keyName;
    private List<ERDiagramDataSetDTO> dataSets = new ArrayList<>();

    public ERDiagramDTO() {
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public List<ERDiagramDataSetDTO> getDataSets() {
        return dataSets;
    }

    public void setDataSets(List<ERDiagramDataSetDTO> dataSets) {
        this.dataSets = dataSets == null ? new ArrayList<>() : dataSets;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ERDiagramDTO that = (ERDiagramDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getDataSets(), that.getDataSets());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getDataSets());
    }

    @Override
    public String toString() {
        return "ERDiagramDTO{" +
                "keyName='" + keyName + '\'' +
                ", dataSets=" + dataSets +
                '}';
    }
}
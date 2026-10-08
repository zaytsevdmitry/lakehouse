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

package org.lakehouse.client.api.dto.configs.datalineage;

import java.io.Serializable;
import java.util.Objects;

/**
 * Visual lineage diagram over {@code kind: DataSet} entities. The document only
 * stores which data sets are placed on the canvas, while the arrows between them
 * are derived from the {@code sources} map of each referenced data set. The free
 * form {@code spec} mirrors the {@code {datasets: [...], layout: {positions}}}
 * structure of the YAML file; it is rendered by the dedicated diagram editor and
 * is never bound by the configuration service because the kind is not config.
 */
public class DataLineageDiagramDTO implements Serializable {

    private String keyName;
    private Object spec;

    public DataLineageDiagramDTO() {
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Object getSpec() {
        return spec;
    }

    public void setSpec(Object spec) {
        this.spec = spec;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataLineageDiagramDTO that = (DataLineageDiagramDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getSpec(), that.getSpec());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getSpec());
    }

    @Override
    public String toString() {
        return "DataLineageDiagramDTO{" +
                "keyName='" + keyName + '\'' +
                ", spec=" + spec +
                '}';
    }
}
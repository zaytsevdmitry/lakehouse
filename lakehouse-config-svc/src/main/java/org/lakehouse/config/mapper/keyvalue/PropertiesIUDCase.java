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

package org.lakehouse.config.mapper.keyvalue;

import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.ArrayList;
import java.util.List;

public class PropertiesIUDCase {
    private List<KeyValueAbstract>  toBeDeleted = new ArrayList<>();
    private List<KeyValueAbstract>  toBeUpdated = new ArrayList<>();
    private List<KeyValueAbstract>  toBeInserted = new ArrayList<>();

    public List<KeyValueAbstract> getToBeDeleted() {
        return toBeDeleted;
    }

    public void setToBeDeleted(List<KeyValueAbstract> toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public List<KeyValueAbstract> getToBeUpdated() {
        return toBeUpdated;
    }

    public void setToBeUpdated(List<KeyValueAbstract> toBeUpdated) {
        this.toBeUpdated = toBeUpdated;
    }

    public List<KeyValueAbstract> getToBeInserted() {
        return toBeInserted;
    }

    public void setToBeInserted(List<KeyValueAbstract> toBeInserted) {
        this.toBeInserted = toBeInserted;
    }
    public List<KeyValueAbstract> getToBeSaved(){
        List<KeyValueAbstract> result = new ArrayList<>();
        result.addAll(getToBeUpdated());
        result.addAll(getToBeInserted());
        return result;
    }
}

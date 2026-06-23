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

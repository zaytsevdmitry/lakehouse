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

package org.lakehouse.state.factory;

import org.lakehouse.state.entity.DataSetState;

import java.util.List;

public class MergeResult {
    private List<DataSetState> afterChange;
    private List<DataSetState> forRemove;

    public MergeResult(List<DataSetState> afterChange, List<DataSetState> forRemove) {
        this.afterChange = afterChange;
        this.forRemove = forRemove;
    }

    public List<DataSetState> getAfterChange() {
        return afterChange;
    }

    public void setAfterChange(List<DataSetState> afterChange) {
        this.afterChange = afterChange;
    }

    public List<DataSetState> getForRemove() {
        return forRemove;
    }

    public void setForRemove(List<DataSetState> forRemove) {
        this.forRemove = forRemove;
    }
}

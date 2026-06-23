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

package org.lakehouse.validator.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ValidationResult {
    //todo move to common and apply in client
    private boolean valid;
    private List<String> descriptions = new ArrayList<>();

    public ValidationResult(boolean valid, List<String> descriptions) {
        this.valid = valid;
        this.descriptions = descriptions;
    }

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public ValidationResult() {
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationResult that = (ValidationResult) o;
        return isValid() == that.isValid() && Objects.equals(getDescriptions(), that.getDescriptions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(isValid(), getDescriptions());
    }
}

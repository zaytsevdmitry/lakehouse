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

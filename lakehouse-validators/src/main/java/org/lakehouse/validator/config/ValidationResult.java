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

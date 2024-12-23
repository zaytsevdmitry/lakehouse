package org.lakehouse.cli.api.dto.configs;

import java.util.Objects;

public class DagEdgeDTO {
    private String  from;
    private String to;

    public DagEdgeDTO() {
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DagEdgeDTO that = (DagEdgeDTO) o;
        return Objects.equals(getFrom(), that.getFrom()) && Objects.equals(getTo(), that.getTo());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFrom(), getTo());
    }
}

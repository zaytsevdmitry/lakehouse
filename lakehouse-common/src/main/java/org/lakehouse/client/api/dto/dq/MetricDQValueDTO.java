package org.lakehouse.client.api.dto.dq;

import java.io.Serial;
import java.io.Serializable;

public final class MetricDQValueDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -8715383864728411678L;
    private Long metricId;
    private String subMetricName;
    private Long value;

    public MetricDQValueDTO() {
    }

    public Long getMetricId() {
        return metricId;
    }

    public void setMetricId(Long metricId) {
        this.metricId = metricId;
    }

    public String getSubMetricName() {
        return subMetricName;
    }

    public void setSubMetricName(String subMetricName) {
        this.subMetricName = subMetricName;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }
};

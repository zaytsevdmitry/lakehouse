package org.lakehouse.client.api.dto.dq;

import org.lakehouse.client.api.constant.Status;

public record MetricDQStatusTestSetDTO(
    Long id,
    Long metricId,
    String testSetKeyName,
    Status.DQMetric status)
{}

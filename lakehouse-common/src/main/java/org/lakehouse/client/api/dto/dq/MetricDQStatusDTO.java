package org.lakehouse.client.api.dto.dq;

import org.lakehouse.client.api.constant.Status;

public record MetricDQStatusDTO(
    Long metricId,
    String catalogName,
    String dataBaseSchemaName,
    String tableName,
    String currentDateTime,
    String targetDateTime,
    String intervalStartDateTime,
    String intervalEndDateTime,
    Status.DQMetric status,
    String metricKeyName
){};

package org.lakehouse.client.api.dto.dq;

import org.lakehouse.client.api.constant.Status;

import java.time.OffsetDateTime;

public record MetricDQStatusDTO(
    Long Id,
    String catalogName,
    String dataBaseSchemaName,
    String tableName,
    OffsetDateTime currentDateTime,
    OffsetDateTime targetDateTime,
    OffsetDateTime intervalStartDateTime,
    OffsetDateTime intervalEndDateTime,
    Status.DQMetric status,
    String metricKeyName
){};

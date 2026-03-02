package org.lakehouse.client.api.dto.dq;

import java.time.OffsetDateTime;

public record MetricDQValueDTO(
    Long Id,
    String catalogName,
    String dataBaseSchemaName,
    String tableName,
    OffsetDateTime currentDateTime,
    OffsetDateTime targetDateTime,
    OffsetDateTime intervalStartDateTime,
    OffsetDateTime intervalEndDateTime,
    Long value,
    String metricKeyName
){};

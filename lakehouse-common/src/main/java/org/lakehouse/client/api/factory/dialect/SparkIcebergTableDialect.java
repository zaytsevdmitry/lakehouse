package org.lakehouse.client.api.factory.dialect;

import org.lakehouse.client.api.exception.DDLDIalectConstraintException;

public class SparkIcebergTableDialect extends AbstractTableDialect{
    public SparkIcebergTableDialect(TableDialectParameter tableDialectParameter) throws DDLDIalectConstraintException {
        super(tableDialectParameter);
    }
}

package org.lakehouse.client.api.factory.dialect;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.exception.DDLDIalectConstraintException;

import java.util.Set;

public abstract class JdbcAbstactTableDialect extends AbstractTableDialect {

    public JdbcAbstactTableDialect(TableDialectParameter tableDialectParameter) throws DDLDIalectConstraintException {
        super(tableDialectParameter);
    }
}

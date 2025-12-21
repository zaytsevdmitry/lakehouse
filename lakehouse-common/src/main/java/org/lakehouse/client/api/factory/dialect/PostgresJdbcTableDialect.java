package org.lakehouse.client.api.factory.dialect;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.exception.DDLDIalectConstraintException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PostgresJdbcTableDialect extends JdbcAbstactTableDialect{
    public PostgresJdbcTableDialect(TableDialectParameter tableDialectParameter) throws DDLDIalectConstraintException {
        super(tableDialectParameter);
    }



}

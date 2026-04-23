package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceProperty;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public  class DataSourcePropertyKeyValueEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final DataSource dataSource;

    public DataSourcePropertyKeyValueEntitySpecifier(
            JpaRepository jpaRepository,
            DataSource dataSource) {
        super(jpaRepository);
        this.dataSource = dataSource;
    }


    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        DataSourceProperty result = ((DataSourceProperty)keyValueAbstract);
        result.setDataSource(dataSource);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return DataSourceProperty.class;
    }
}

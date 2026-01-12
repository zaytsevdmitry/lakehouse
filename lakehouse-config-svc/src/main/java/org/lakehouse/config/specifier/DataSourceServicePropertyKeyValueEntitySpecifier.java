package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.datasource.DataSourceSvcItem;
import org.lakehouse.config.entities.datasource.DataSourceSvcItemProperty;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public  class DataSourceServicePropertyKeyValueEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final DataSourceSvcItem dataSourceSvcItem;

    public DataSourceServicePropertyKeyValueEntitySpecifier(
            JpaRepository jpaRepository,
            DataSourceSvcItem dataSourceSvcItem) {
        super(jpaRepository);
        this.dataSourceSvcItem = dataSourceSvcItem;
    }


    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        DataSourceSvcItemProperty result = ((DataSourceSvcItemProperty)keyValueAbstract);
        result.setDataSourceSvcItem(dataSourceSvcItem);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return DataSourceSvcItemProperty.class;
    }
}

package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.entities.datasource.SQLTemplate;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public  class SQLTemplateEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final Driver driver;
    private final DataSource dataSource;
    private final DataSet dataSet;

    public SQLTemplateEntitySpecifier(
            JpaRepository jpaRepository,
            Driver driver,
            DataSource dataSource,
            DataSet dataSet) {
        super(jpaRepository);
        this.driver = driver;
        this.dataSource = dataSource;
        this.dataSet = dataSet;
    }


    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        SQLTemplate result = (SQLTemplate) keyValueAbstract;
        result.setDriver(driver);
        result.setDataSource(dataSource);
        result.setDataSet(dataSet);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return SQLTemplate.class;
    }
}

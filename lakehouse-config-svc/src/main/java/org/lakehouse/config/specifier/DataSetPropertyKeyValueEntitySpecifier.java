package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetProperty;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public class DataSetPropertyKeyValueEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final DataSet dataSet;

    public DataSetPropertyKeyValueEntitySpecifier(
            JpaRepository jpaRepository,
            DataSet dataSet) {
        super(jpaRepository);
        this.dataSet = dataSet;
    }

    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        DataSetProperty result = ((DataSetProperty)keyValueAbstract);
        result.setDataSet(dataSet);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return DataSetProperty.class;
    }
}

package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.entities.dataset.DataSetSourceProperty;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public class DataSetSourcePropertyKeyValueEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final DataSetSource dataSetSource;

    public DataSetSourcePropertyKeyValueEntitySpecifier(
            JpaRepository jpaRepository,
            DataSetSource dataSetSource) {
        super(jpaRepository);
        this.dataSetSource = dataSetSource;
    }

    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        DataSetSourceProperty result = ((DataSetSourceProperty)keyValueAbstract);
        result.setDataSetSource(dataSetSource);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return DataSetSourceProperty.class;
    }
}

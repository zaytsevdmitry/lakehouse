/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

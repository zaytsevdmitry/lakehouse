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
import org.lakehouse.config.entities.SQLTemplate;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.Driver;
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

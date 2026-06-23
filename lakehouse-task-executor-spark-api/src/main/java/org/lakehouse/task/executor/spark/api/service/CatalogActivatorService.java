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

package org.lakehouse.task.executor.spark.api.service;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class CatalogActivatorService {
    private final Logger log = LoggerFactory.getLogger(CatalogActivatorService.class);

    private final SparkSession sparkSession;

    public CatalogActivatorService(
            SparkSession sparkSession) {
        this.sparkSession = sparkSession;

    }

    private void activateOne(String catalogName) {
        log.info("Activate DataSource to Catalog with name {}", catalogName);
        sparkSession.catalog().setCurrentCatalog(catalogName);
    }

    public  void activate(SourceConfDTO sourceConfDTO){
        // Activate catalogs
        log.info("Before activate catalogs");
        sparkSession.catalog().listCatalogs().show();
        for (DataSourceDTO dataSourceDTO:sourceConfDTO.getDataSources().values()){
            activateOne(dataSourceDTO.getCatalogKeyName());
        }
        log.info("After activate catalogs");
        sparkSession.catalog().listCatalogs().show();
        //  to target catalog
        log.info("Switch to target catalog");
        sparkSession.catalog().setCurrentCatalog(sourceConfDTO.getTargetDataSource().getCatalogKeyName());
    }
}

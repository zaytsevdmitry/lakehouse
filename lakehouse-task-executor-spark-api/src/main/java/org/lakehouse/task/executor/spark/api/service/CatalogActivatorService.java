/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

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

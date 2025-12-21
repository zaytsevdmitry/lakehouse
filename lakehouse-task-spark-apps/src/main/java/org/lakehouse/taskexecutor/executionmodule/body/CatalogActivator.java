package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CatalogActivator {
    private final Logger log = LoggerFactory.getLogger(CatalogActivator.class);

    private final SparkSession sparkSession;

    public CatalogActivator(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    private void activateOne(DataSourceDTO dataSourceDTO){
        log.info("Activate DataSource to Catalog with name {}", dataSourceDTO.getKeyName());
        sparkConfSet(dataSourceDTO.getProperties());
        sparkSession.catalog().setCurrentCatalog(dataSourceDTO.getKeyName());

    }
    public  void activate(DataSourceDTO dataSourceDTO){
        // Activate catalogs
        sparkSession.catalog().listCatalogs().show();
        activateOne(dataSourceDTO);
        sparkSession.catalog().listCatalogs().show();

        // back to default catalog
        sparkSession.catalog().setCurrentCatalog("spark_catalog");

    }
    public  void activate(List<DataSourceDTO> dataSourceDTOS){
        // Activate catalogs
        sparkSession.catalog().listCatalogs().show();
        for (DataSourceDTO dataSourceDTO:dataSourceDTOS){
            activateOne(dataSourceDTO);
        }
        sparkSession.catalog().listCatalogs().show();
        // back to default catalog
        sparkSession.catalog().setCurrentCatalog("spark_catalog");
    }

    private void sparkConfSet(Map<String,String> conf){
        conf.entrySet()
                .stream()
                .filter(stringStringEntry -> stringStringEntry.getKey().startsWith("spark.sql.catalog."))
                .forEach(stringStringEntry ->
                        sparkSession.conf().set(stringStringEntry.getKey(),stringStringEntry.getValue()));
    }
}

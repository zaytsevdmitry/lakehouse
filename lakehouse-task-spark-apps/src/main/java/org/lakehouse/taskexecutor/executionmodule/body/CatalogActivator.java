package org.lakehouse.taskexecutor.executionmodule.body;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CatalogActivator {
    private final Logger log = LoggerFactory.getLogger(CatalogActivator.class);

    private final SparkSession sparkSession;
    private final Jinjava jinjava;

    public CatalogActivator(SparkSession sparkSession, Jinjava jinjava) {
        this.sparkSession = sparkSession;
        this.jinjava = jinjava;
    }

    private void activateOne(DriverDTO driverDTO,DataSourceDTO dataSourceDTO){
        log.info("Activate DataSource to Catalog with name {}", dataSourceDTO.getKeyName());
        Map<String,Object> localContext = new HashMap<>();
        localContext.put(SystemVarKeys.DRIVER_KEY, driverDTO);
        localContext.put(SystemVarKeys.SERVICE_KEY, dataSourceDTO.getServices().get(0));
        localContext.put(SystemVarKeys.DATASOURCE_KEY, dataSourceDTO);

        sparkConfSet(
        dataSourceDTO.getProperties().entrySet().stream()
                .filter(e -> e.getKey().startsWith("spark.sql.catalog"))
                .peek(e -> e.setValue( jinjava.render(e.getValue(),localContext)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
        sparkSession.catalog().setCurrentCatalog(dataSourceDTO.getKeyName());

    }
    public  void activate(DriverDTO driverDTO, DataSourceDTO dataSourceDTO){


        // Activate catalogs

        sparkSession.catalog().listCatalogs().show();
        activateOne(driverDTO,dataSourceDTO);
        sparkSession.catalog().listCatalogs().show();

        // back to default catalog
        sparkSession.catalog().setCurrentCatalog("spark_catalog");

    }
    public  void activate(List<DataSourceDTO> dataSourceDTOS, Map<String,DriverDTO> driverDTOs){
        // Activate catalogs
        sparkSession.catalog().listCatalogs().show();
        for (DataSourceDTO dataSourceDTO:dataSourceDTOS){
            activateOne(driverDTOs.get(dataSourceDTO.getDriverKeyName()), dataSourceDTO);
        }
        sparkSession.catalog().listCatalogs().show();
        // back to default catalog
        sparkSession.catalog().setCurrentCatalog("spark_catalog");
    }

    private void sparkConfSet(Map<String,String> conf){
        conf.entrySet()
                .stream()
                .filter(stringStringEntry -> stringStringEntry.getKey().startsWith("spark.sql.catalog."))
                .forEach(stringStringEntry -> {
                    log.info("Spark cond add entry {} -> {} ", stringStringEntry.getKey(),stringStringEntry.getValue());
                    sparkSession.conf().set(stringStringEntry.getKey(), stringStringEntry.getValue());
                }
                );
    }
}

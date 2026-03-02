package org.lakehouse.config.service.datasource;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceSvcItem;
import org.lakehouse.config.entities.datasource.DataSourceSvcItemProperty;
import org.lakehouse.config.exception.DataSourceNotFoundException;
import org.lakehouse.config.exception.DataSourceServiceNotFoundException;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.datasource.DataSourcePropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
import org.lakehouse.config.repository.datasource.DataSourceSvcItemPropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceSvcItemRepository;
import org.lakehouse.config.specifier.DataSourcePropertyKeyValueEntitySpecifier;
import org.lakehouse.config.specifier.DataSourceServicePropertyKeyValueEntitySpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataSourceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSourceRepository dataSourceRepository;
    private final DataSourcePropertyRepository dataSourcePropertyRepository;
    private final DataSourceSvcItemRepository dataSourceSvcItemRepository;
    private final DataSourceSvcItemPropertyRepository dataSourceSvcItemPropertyRepository;
    private final DriverService driverService;
    private final SQLTemplateService sqlTemplateService;
    public DataSourceService(
            DataSourceRepository dataSourceRepository,
            DataSourcePropertyRepository dataSourcePropertyRepository,
            DataSourceSvcItemRepository dataSourceSvcItemRepository,
            DataSourceSvcItemPropertyRepository dataSourceSvcItemPropertyRepository,
            DriverService driverService,
            SQLTemplateService sqlTemplateService) {
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourcePropertyRepository = dataSourcePropertyRepository;
        this.dataSourceSvcItemRepository = dataSourceSvcItemRepository;
        this.dataSourceSvcItemPropertyRepository = dataSourceSvcItemPropertyRepository;
        this.driverService = driverService;
        this.sqlTemplateService = sqlTemplateService;
    }

    private ServiceDTO findDataSourceService(String dataSourceKeyName){
        DataSourceSvcItem dataSourceSvcItem = dataSourceSvcItemRepository
                .findByDataSourceKeyName(dataSourceKeyName)
                .orElseThrow(() -> new DataSourceServiceNotFoundException(dataSourceKeyName));

        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setHost(dataSourceSvcItem.getHost());
        serviceDTO.setPort(dataSourceSvcItem.getPort());
        serviceDTO.setUrn(dataSourceSvcItem.getUrn());
        serviceDTO.setProperties(
                dataSourceSvcItemPropertyRepository
                        .findByDataSourceSvcItemId(dataSourceSvcItem.getId())
                        .stream()
                        .collect(Collectors.toMap(DataSourceSvcItemProperty::getKey, DataSourceSvcItemProperty::getValue))
        );
        return serviceDTO;
    }

    private DataSourceDTO mapDataSourceToDTO(DataSource dataSource) {
        DataSourceDTO result = new DataSourceDTO();
        result.setKeyName(dataSource.getKeyName());
        result.setDescription(dataSource.getDescription());
        result.setDriverKeyName(dataSource.getDriver().getKeyName());
        result.setService(findDataSourceService(dataSource.getKeyName()));
        result.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(dataSource));
        result.setCatalogKeyName(dataSource.getCatalogKeyName());
        return result;
    }


    private DataSource mapDataSourceToEntity(DataSourceDTO dataSourceDTO) {
        DataSource result = new DataSource();
        result.setKeyName(dataSourceDTO.getKeyName());
        result.setDescription(dataSourceDTO.getDescription());
        result.setDriver(driverService.findDriverById(dataSourceDTO.getDriverKeyName()));
        result.setCatalogKeyName(dataSourceDTO.getCatalogKeyName());
        return result;
    }

    private DataSourceSvcItem mapServiceToEntity(
            DataSource dataSource,
            ServiceDTO serviceDTO) {
        DataSourceSvcItem result = dataSourceSvcItemRepository
                .findByDataSourceKeyName(dataSource.getKeyName()).orElse(new DataSourceSvcItem());

        result.setHost(serviceDTO.getHost());
        result.setPort(serviceDTO.getPort());
        result.setUrn(serviceDTO.getUrn());
        result.setDataSource(dataSource);
        return result;
    }

    public List<DataSourceDTO> findAll() {
        return dataSourceRepository.findAll().stream().map(this::mapDataSourceToDTO).toList();
    }

    private void saveProperties(
            DataSource dataSource,
            Map<String,String> newProperties){

        new KeyValueEntityMerger(
                new DataSourcePropertyKeyValueEntitySpecifier(dataSourcePropertyRepository,dataSource))
                .mergeAbstractKeyValues(dataSourcePropertyRepository
                        .findByDataSourceKeyName(dataSource.getKeyName())
                        .stream()
                        .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                        .toList(),
                        newProperties);

    }


    private void saveSvcProperty(
            DataSourceSvcItem dataSourceSvcItem,
            Map<String,String> newProperties){
        new KeyValueEntityMerger(
                new DataSourceServicePropertyKeyValueEntitySpecifier(dataSourceSvcItemPropertyRepository,dataSourceSvcItem))
                .mergeAbstractKeyValues(
                        dataSourceSvcItemPropertyRepository
                                .findByDataSourceSvcItemId(dataSourceSvcItem.getId())
                                .stream()
                                .map(dataSourceSvcItemProperty -> (KeyValueAbstract) dataSourceSvcItemProperty)
                                .toList(),
                        newProperties);

    }
    @Transactional
    public DataSourceDTO save(DataSourceDTO dataSourceDTO) {
        DataSource dataSource = dataSourceRepository.save(mapDataSourceToEntity(dataSourceDTO));

        // services
        //todo rewrite to merge
        DataSourceSvcItem dataSourceSvcItem =
                dataSourceSvcItemRepository.save(
                        mapServiceToEntity(dataSource,dataSourceDTO.getService()));


         saveSvcProperty(dataSourceSvcItem,dataSourceDTO.getService().getProperties());

        //sql templates
        sqlTemplateService.save(dataSource,dataSourceDTO.getSqlTemplate());

        return mapDataSourceToDTO(dataSource);
    }

    public DataSourceDTO findById(String name) {
        return mapDataSourceToDTO(
                dataSourceRepository.findById(name).orElseThrow(() -> new DataSourceNotFoundException(name)));
    }

    @Transactional
    public void deleteById(String name) {
        dataSourceRepository.deleteById(name);
    }
}

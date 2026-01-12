package org.lakehouse.config.service.datasource;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceSvcItem;
import org.lakehouse.config.entities.datasource.DataSourceSvcItemProperty;
import org.lakehouse.config.exception.DataSourceNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.SQLTemplateRepository;
import org.lakehouse.config.repository.datasource.DataSourcePropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
import org.lakehouse.config.repository.datasource.DataSourceSvcItemPropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceSvcItemRepository;
import org.lakehouse.config.specifier.DataSourcePropertyKeyValueEntitySpecifier;
import org.lakehouse.config.specifier.DataSourceServicePropertyKeyValueEntitySpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataSourceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Mapper mapper;
    private final DataSourceRepository dataSourceRepository;
    private final DataSourcePropertyRepository dataSourcePropertyRepository;
    private final DataSourceSvcItemRepository dataSourceSvcItemRepository;
    private final DataSourceSvcItemPropertyRepository dataSourceSvcItemPropertyRepository;
    private final SQLTemplateRepository sqlTemplateRepository;
    private final DriverService driverService;
    private final SQLTemplateService sqlTemplateService;
    public DataSourceService(Mapper mapper,
                             DataSourceRepository dataSourceRepository,
                             DataSourcePropertyRepository dataSourcePropertyRepository,
                             DataSourceSvcItemRepository dataSourceSvcItemRepository,
                             DataSourceSvcItemPropertyRepository dataSourceSvcItemPropertyRepository,
                             SQLTemplateRepository sqlTemplateRepository,
                             DriverService driverService, SQLTemplateService sqlTemplateService) {
        this.mapper = mapper;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourcePropertyRepository = dataSourcePropertyRepository;
        this.dataSourceSvcItemRepository = dataSourceSvcItemRepository;
        this.dataSourceSvcItemPropertyRepository = dataSourceSvcItemPropertyRepository;
        this.sqlTemplateRepository = sqlTemplateRepository;
        this.driverService = driverService;
        this.sqlTemplateService = sqlTemplateService;
    }

    private DataSourceDTO mapDataSourceToDTO(DataSource dataSource) {
        DataSourceDTO result = new DataSourceDTO();
        result.setKeyName(dataSource.getKeyName());
        result.setDescription(dataSource.getDescription());
        result.setDriverKeyName(dataSource.getDriver().getKeyName());
        Map<String, String> properties = new HashMap<>();
        dataSourcePropertyRepository.findByDataSourceKeyName(dataSource.getKeyName())
                .forEach(dataSourceProperty -> properties.put(dataSourceProperty.getKey(), dataSourceProperty.getValue()));
        result.setProperties(properties);
        result.setServices(dataSourceSvcItemRepository
                .findByDataSourceKeyName(dataSource.getKeyName())
                .stream().map(dataSourceService -> {
                    ServiceDTO serviceDTO = new ServiceDTO();
                    serviceDTO.setHost(dataSourceService.getHost());
                    serviceDTO.setPort(dataSourceService.getPort());
                    serviceDTO.setUrn(dataSourceService.getUrn());
                    serviceDTO.setProperties(
                            dataSourceSvcItemPropertyRepository
                                    .findByDataSourceSvcItemId(dataSourceService.getId())
                                    .stream()
                                    .collect(Collectors.toMap(DataSourceSvcItemProperty::getKey, DataSourceSvcItemProperty::getValue))
                    );
                    return serviceDTO;
                }).toList());
        result.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(dataSource));
        return result;
    }


    private DataSource mapDataSourceToEntity(DataSourceDTO dataSourceDTO) {
        DataSource result = new DataSource();
        result.setKeyName(dataSourceDTO.getKeyName());
        result.setDescription(dataSourceDTO.getDescription());
        result.setDriver(driverService.findDriverById(dataSourceDTO.getDriverKeyName()));
        return result;
    }

    private DataSourceSvcItem mapServiceToEntity(DataSource dataSource, ServiceDTO serviceDTO) {
        DataSourceSvcItem result = new DataSourceSvcItem();
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

        saveProperties(dataSource,dataSourceDTO.getProperties());

        // services
        //todo rewrite to merge
        dataSourceSvcItemRepository
                .deleteAll(
                        dataSourceSvcItemRepository
                                .findByDataSourceKeyName(dataSourceDTO.getKeyName()));

        // property
        dataSourceDTO
                .getServices()
                .forEach(serviceDTO -> {
                    DataSourceSvcItem dataSourceSvcItem =
                            dataSourceSvcItemRepository.save(mapServiceToEntity(dataSource, serviceDTO));
                    saveSvcProperty(dataSourceSvcItem,serviceDTO.getProperties());
                });

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

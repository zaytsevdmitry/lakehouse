package org.lakehouse.config.service.datasource;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceProperty;
import org.lakehouse.config.entities.datasource.DataSourceServiceProperty;
import org.lakehouse.config.exception.DataStoreNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifier;
import org.lakehouse.config.mapper.keyvalue.PropertiesIUDCase;
import org.lakehouse.config.repository.datasource.DataSourcePropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceRepository;
import org.lakehouse.config.repository.datasource.DataSourceServicePropertyRepository;
import org.lakehouse.config.repository.datasource.DataSourceServiceRepository;
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
    private final DataSourceServiceRepository dataSourceServiceRepository;
    private final DataSourceServicePropertyRepository dataSourceServicePropertyRepository;

    public DataSourceService(Mapper mapper, DataSourceRepository dataSourceRepository,
                             DataSourcePropertyRepository dataSourcePropertyRepository, DataSourceServiceRepository dataSourceServiceRepository, DataSourceServicePropertyRepository dataSourceServicePropertyRepository) {
        this.mapper = mapper;
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourcePropertyRepository = dataSourcePropertyRepository;
        this.dataSourceServiceRepository = dataSourceServiceRepository;
        this.dataSourceServicePropertyRepository = dataSourceServicePropertyRepository;
    }

    private DataSourceDTO mapDataSourceToDTO(DataSource dataSource) {
        DataSourceDTO result = new DataSourceDTO();
        result.setKeyName(dataSource.getKeyName());
        result.setDescription(dataSource.getDescription());
        result.setEngineType(dataSource.getDataSourceType());
        result.setEngine(dataSource.getDataSourceServiceType());
        Map<String, String> properties = new HashMap<>();
        dataSourcePropertyRepository.findByDataSourceKeyName(dataSource.getKeyName())
                .forEach(dataSourceProperty -> properties.put(dataSourceProperty.getKey(), dataSourceProperty.getValue()));
        result.setProperties(properties);
        result.setServices(dataSourceServiceRepository
                .findByDataSourceKeyName(dataSource.getKeyName())
                .stream().map(dataSourceService -> {
                    ServiceDTO serviceDTO = new ServiceDTO();
                    serviceDTO.setHost(dataSourceService.getHost());
                    serviceDTO.setPort(dataSourceService.getPort());
                    serviceDTO.setUrn(dataSourceService.getUrn());
                    serviceDTO.setProperties(
                            dataSourceServicePropertyRepository
                                    .findByDataSourceServiceId(dataSourceService.getId())
                                    .stream()
                                    .collect(Collectors.toMap(DataSourceServiceProperty::getKey, DataSourceServiceProperty::getValue))
                    );
                    return serviceDTO;
                }).toList());
        return result;
    }

    private DataSource mapDataSourceToEntity(DataSourceDTO dataSource) {
        DataSource result = new DataSource();
        result.setKeyName(dataSource.getKeyName());
        result.setDescription(dataSource.getDescription());
        result.setDataSourceType(dataSource.getEngineType());
        result.setDataSourceServiceType(dataSource.getEngine());
        return result;
    }

    private org.lakehouse.config.entities.datasource.DataSourceService mapServiceToEntity(DataSource dataSource, ServiceDTO serviceDTO) {
        org.lakehouse.config.entities.datasource.DataSourceService result = new org.lakehouse.config.entities.datasource.DataSourceService();
        result.setHost(serviceDTO.getHost());
        result.setPort(serviceDTO.getPort());
        result.setUrn(serviceDTO.getUrn());
        result.setDataSource(dataSource);
        return result;
    }

    public List<DataSourceDTO> findAll() {
        return dataSourceRepository.findAll().stream().map(this::mapDataSourceToDTO).toList();
    }

    private void mergeDataSourceProperties(DataSource dataSource, Map<String,String> properties){

        new KeyValueEntityMerger(
                new DataSourcePropertyKeyValueEntitySpecifier(dataSourcePropertyRepository,dataSource))
                .mergeAbstractKeyValues(
                        dataSourcePropertyRepository
                                .findByDataSourceKeyName(dataSource.getKeyName())
                                .stream()
                                .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                                .toList(),
                        properties);
    }
    @Transactional
    public DataSourceDTO save(DataSourceDTO dataSourceDTO) {
        DataSource dataSource = dataSourceRepository.save(mapDataSourceToEntity(dataSourceDTO));


        mergeDataSourceProperties(dataSource,dataSourceDTO.getProperties());

        // services
        dataSourceServiceRepository
                .deleteAll(
                        dataSourceServiceRepository
                                .findByDataSourceKeyName(dataSourceDTO.getKeyName()));


        dataSourceDTO
                .getServices()
                .forEach(serviceDTO -> {
                    org.lakehouse.config.entities.datasource.DataSourceService dataSourceService =
                            dataSourceServiceRepository.save(mapServiceToEntity(dataSource, serviceDTO));
                    serviceDTO.getProperties().entrySet().forEach(s -> {
                        DataSourceServiceProperty property = new DataSourceServiceProperty();
                        property.setDataSourceService(dataSourceService);
                        property.setKey(s.getKey());
                        property.setValue(s.getValue());
                        dataSourceServicePropertyRepository.save(property);
                    });

                });


        return mapDataSourceToDTO(dataSource);
    }

    public DataSourceDTO findById(String name) {
        return mapDataSourceToDTO(
                dataSourceRepository.findById(name).orElseThrow(() -> new DataStoreNotFoundException(name)));
    }

    @Transactional
    public void deleteById(String name) {
        dataSourceRepository.deleteById(name);
    }
}

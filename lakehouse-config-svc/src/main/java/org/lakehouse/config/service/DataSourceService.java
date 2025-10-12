package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.config.entities.datasource.DataSource;
import org.lakehouse.config.entities.datasource.DataSourceProperty;
import org.lakehouse.config.entities.datasource.DataSourceServiceProperty;
import org.lakehouse.config.exception.DataStoreNotFoundException;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DataSourceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DataSourceRepository dataSourceRepository;
    private final DataSourcePropertyRepository dataSourcePropertyRepository;
    private final DataSourceServiceRepository dataSourceServiceRepository;
    private final DataSourceServicePropertyRepository dataSourceServicePropertyRepository;

    public DataSourceService(DataSourceRepository dataSourceRepository,
                             DataSourcePropertyRepository dataSourcePropertyRepository, DataSourceServiceRepository dataSourceServiceRepository, DataSourceServicePropertyRepository dataSourceServicePropertyRepository) {
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourcePropertyRepository = dataSourcePropertyRepository;
        this.dataSourceServiceRepository = dataSourceServiceRepository;
        this.dataSourceServicePropertyRepository = dataSourceServicePropertyRepository;
    }

    private DataSourceDTO mapDataSourceToDTO(DataSource dataSource) {
        DataSourceDTO result = new DataSourceDTO();
        result.setKeyName(dataSource.getKeyName());
        result.setDescription(dataSource.getDescription());
        result.setDataSourceType(dataSource.getDataSourceType());
        result.setDataSourceServiceType(dataSource.getDataSourceServiceType());
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
        result.setDataSourceType(dataSource.getDataSourceType());
        result.setDataSourceServiceType(dataSource.getDataSourceServiceType());
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

    @Transactional
    public DataSourceDTO save(DataSourceDTO dataSourceDTO) {
        DataSource dataSource = dataSourceRepository.save(mapDataSourceToEntity(dataSourceDTO));
        List<DataSourceProperty> dataSourcePropertiesBeforeChange = dataSourcePropertyRepository
                .findByDataSourceKeyName(dataSource.getKeyName());

        dataSourcePropertiesBeforeChange.forEach(dataSourceProperty -> {
            if (!dataSourceDTO.getProperties().containsKey(dataSourceProperty.getKey())) {
                dataSourcePropertyRepository.delete(dataSourceProperty);
            }
        });
        // properties
        dataSourceDTO.getProperties().entrySet().stream().map(stringStringEntry -> {
            Optional<DataSourceProperty> optionalDataStoreProperty = dataSourcePropertyRepository
                    .findByKeyAndDataSourceKeyName(stringStringEntry.getKey(), dataSourceDTO.getKeyName());
            if (optionalDataStoreProperty.isPresent()) {
                if (!optionalDataStoreProperty.get().getValue().equals(stringStringEntry.getValue())) {
                    return optionalDataStoreProperty.get(); // dataSourcePropertyRepository.save(optionalDataStoreProperty.get());
                } else {
                    DataSourceProperty dataSourceProperty = optionalDataStoreProperty.get();
                    dataSourceProperty.setValue(stringStringEntry.getValue());
                    return dataSourceProperty;
                }
            } else {
                DataSourceProperty dataSourceProperty = new DataSourceProperty();
                dataSourceProperty.setDataSource(dataSource);
                dataSourceProperty.setKey(stringStringEntry.getKey());
                dataSourceProperty.setValue(stringStringEntry.getValue());
                return dataSourceProperty;
            }
        }).toList().forEach(dataSourcePropertyRepository::save);
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

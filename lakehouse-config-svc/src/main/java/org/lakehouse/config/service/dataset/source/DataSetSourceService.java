package org.lakehouse.config.service.dataset.source;

import org.lakehouse.client.api.dto.configs.dataset.DataSetSourceDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.exception.DataSetNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourcePropertyRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.specifier.DataSetSourcePropertyKeyValueEntitySpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class DataSetSourceService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Mapper mapper;
    private final DataSetRepository dataSetRepository;
    private final DataSetSourceRepository dataSetSourceRepository;
    private final DataSetSourcePropertyRepository dataSetSourcePropertyRepository;

    public DataSetSourceService(
            Mapper mapper, DataSetRepository dataSetRepository,
            DataSetSourceRepository dataSetSourceRepository,
            DataSetSourcePropertyRepository dataSetSourcePropertyRepository) {
        this.mapper = mapper;
        this.dataSetRepository = dataSetRepository;
        this.dataSetSourceRepository = dataSetSourceRepository;
        this.dataSetSourcePropertyRepository = dataSetSourcePropertyRepository;
    }

    public Map<String,DataSetSourceDTO> getDataSetSourceDTOsByDataSetKeyName(String dataSetKeyName){
        Map<String,DataSetSourceDTO> result = new HashMap<>();
        dataSetSourceRepository
                .findByDataSetKeyName(dataSetKeyName)
                .forEach(dataSetSource -> {
                    DataSetSourceDTO dataSetSourceDTO = new DataSetSourceDTO();
                    Map<String, String> props = new HashMap<>();
                    dataSetSourcePropertyRepository
                            .findBySourceId(dataSetSource.getId())
                            .forEach(dataSetSourceProperty ->
                                    props.put(dataSetSourceProperty.getKey(), dataSetSourceProperty.getValue()));
                    dataSetSourceDTO.setProperties(props);
                    result.put(dataSetSource.getSource().getKeyName(),dataSetSourceDTO);
                });
        return result;
    }


    public void save(DataSet dataSet, Map<String,DataSetSourceDTO> dataSetSourceDTOSs){
        logger.info("Saving dataSet={} sources", dataSet.getKeyName());

        Set<String> sourceNames = dataSetSourceDTOSs.keySet();
        dataSetSourceRepository
                .findByDataSetKeyName(dataSet.getKeyName())
                .stream()
                .filter(dataSetSource -> !sourceNames.contains(dataSetSource.getSource().getKeyName()))
                .forEach(dataSetSourceRepository::delete);


        sourceNames.forEach(sourceKeyName -> {

            DataSetSource dataSetSource = dataSetSourceRepository
                    .findByDataSetKeyNameAndSource(dataSet.getKeyName(),sourceKeyName)
                    .orElse( new DataSetSource());

            dataSetSource.setDataSet(dataSet);

            dataSetSource
                    .setSource(
                            dataSetRepository
                                    .findById(sourceKeyName)
                                    .orElseThrow(() -> new DataSetNotFoundException(
                                            String.format("DataSet %s not found", sourceKeyName))));


            DataSetSource resultDataSetSource = dataSetSourceRepository.save(dataSetSource);
            mergeDataSourceProperties(resultDataSetSource, dataSetSourceDTOSs.get(sourceKeyName).getProperties());
        });
    }
    private void mergeDataSourceProperties(DataSetSource dataSetSource, Map<String,String> properties){
        new KeyValueEntityMerger(
                new DataSetSourcePropertyKeyValueEntitySpecifier(dataSetSourcePropertyRepository,dataSetSource))
                .mergeAbstractKeyValues(
                        dataSetSourcePropertyRepository
                                .findBySourceId(dataSetSource.getId())
                                .stream()
                                .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                                .toList(),
                        properties
                );
/*
        PropertiesIUDCase propertiesIUDCase = mapper
                .mergeAbstractKeyValues(
                        dataSetSourcePropertyRepository
                                .findBySourceId(dataSetSource.getId())
                                .stream()
                                .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty )
                                .toList(),
                        properties);

        dataSetSourcePropertyRepository.deleteAll(
                propertiesIUDCase
                        .getToBeDeleted()
                        .stream()
                        .map(keyValueAbstract -> (DataSetSourceProperty) keyValueAbstract)
                        .peek(dataSourceProperty -> dataSourceProperty.setDataSetSource(dataSetSource))
                        .toList());

        dataSetSourcePropertyRepository.saveAll(
                propertiesIUDCase
                        .getToBeSaved()
                        .stream()
                        .map(keyValueAbstract -> (DataSetSourceProperty) keyValueAbstract)
                        .peek(dataSourceProperty -> dataSourceProperty.setDataSetSource(dataSetSource))
                        .toList()
        );*/
    }
}

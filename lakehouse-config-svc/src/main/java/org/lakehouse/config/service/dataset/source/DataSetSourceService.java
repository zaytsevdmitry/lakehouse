package org.lakehouse.config.service.dataset.source;

import org.lakehouse.client.api.dto.configs.dataset.DataSetSourceDTO;
import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.lakehouse.config.exception.DataSetNotFoundException;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntityMerger;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dataset.DataSetSourcePropertyRepository;
import org.lakehouse.config.repository.dataset.DataSetSourceRepository;
import org.lakehouse.config.specifier.DataSetSourcePropertyKeyValueEntitySpecifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataSetSourceService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DataSetRepository dataSetRepository;
    private final DataSetSourceRepository dataSetSourceRepository;
    private final DataSetSourcePropertyRepository dataSetSourcePropertyRepository;

    public DataSetSourceService(
            DataSetRepository dataSetRepository,
            DataSetSourceRepository dataSetSourceRepository,
            DataSetSourcePropertyRepository dataSetSourcePropertyRepository) {

        this.dataSetRepository = dataSetRepository;
        this.dataSetSourceRepository = dataSetSourceRepository;
        this.dataSetSourcePropertyRepository = dataSetSourcePropertyRepository;
    }

    public Map<String, DataSetSourceDTO> findByQualityMetricsConfKeyName(String testConfKeyName){
        return dataSetSourceRepository
                .findByQualityMetricsConfKeyName(testConfKeyName)
                .stream().map(this::dataSetSourceDTOMap)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public Map<String, DataSetSourceDTO> findDataSetSourceDTOsByDataSetKeyName(String dataSetKeyName) {
        return dataSetSourceRepository
                .findByDataSetKeyName(dataSetKeyName)
                .stream().map(this::dataSetSourceDTOMap)
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }

    private Map.Entry<String, DataSetSourceDTO> dataSetSourceDTOMap(DataSetSource dataSetSource){
        DataSetSourceDTO dataSetSourceDTO = new DataSetSourceDTO();
        Map<String, String> props = new HashMap<>();
        dataSetSourcePropertyRepository
                .findBySourceId(dataSetSource.getId())
                .forEach(dataSetSourceProperty ->
                        props.put(dataSetSourceProperty.getKey(), dataSetSourceProperty.getValue()));
        dataSetSourceDTO.setProperties(props);
        return  Map.entry(dataSetSource.getSource().getKeyName() , dataSetSourceDTO);
    }
    public void save(
            DataSet dataSet,
            Map<String, DataSetSourceDTO> dataSetSourceDTOSs) {
        save(
                dataSet,
                null,
                dataSetSourceDTOSs,
                dataSetSourceRepository
                        .findByDataSetKeyName(dataSet.getKeyName()));
    }

    public void save(
            QualityMetricsConf qualityMetricsConf,
            Map<String, DataSetSourceDTO> dataSetSourceDTOSs) {

        save(
                null,
                qualityMetricsConf,
                dataSetSourceDTOSs,
                dataSetSourceRepository
                        .findByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName()));

    }

    private void save(
            DataSet dataSet,
            QualityMetricsConf qualityMetricsConf,
            Map<String, DataSetSourceDTO> dataSetSourceDTOSs,
            List<DataSetSource> dataSetSources) {
        if (dataSet == null && qualityMetricsConf != null)
            logger.info("Saving qualityMetricsConf={} sources", qualityMetricsConf.getKeyName());
        else if (dataSet != null && qualityMetricsConf == null) {
            logger.info("Saving dataSet={} sources", dataSet.getKeyName());
        }

        dataSetSources.forEach(d->logger.info("{}",d));
        Set<String> sourceNames = dataSetSourceDTOSs.keySet();
        Map<String, DataSetSource> dataSetSourceMap = dataSetSources
                .stream()
                .map(dataSetSource -> Map.entry(dataSetSource.getSource().getKeyName(),dataSetSource))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));

        dataSetSources
                .stream()
                .filter(dataSetSource -> !sourceNames.contains(dataSetSource.getSource().getKeyName()))
                .forEach(dataSetSourceRepository::delete);


        sourceNames.forEach(sourceKeyName -> {
            DataSetSource dataSetSource;
            if(dataSetSourceMap.containsKey(sourceKeyName))
                dataSetSource = dataSetSourceMap.get(sourceKeyName);
            else dataSetSource = new DataSetSource();

            dataSetSource.setQualityMetricsConf(qualityMetricsConf);
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

    private void mergeDataSourceProperties(DataSetSource dataSetSource, Map<String, String> properties) {
        new KeyValueEntityMerger(
                new DataSetSourcePropertyKeyValueEntitySpecifier(dataSetSourcePropertyRepository, dataSetSource))
                .mergeAbstractKeyValues(
                        dataSetSourcePropertyRepository
                                .findBySourceId(dataSetSource.getId())
                                .stream()
                                .map(dataSourceProperty -> (KeyValueAbstract) dataSourceProperty)
                                .toList(),
                        properties
                );
    }
}

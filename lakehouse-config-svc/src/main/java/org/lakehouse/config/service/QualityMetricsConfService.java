package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DataSetSourceDTO;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfTestSetDTO;
import org.lakehouse.config.entities.dq.*;
import org.lakehouse.config.exception.QualityMetricsConfNotFoundException;
import org.lakehouse.config.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QualityMetricsConfService {
    private final DataSetRepository dataSetRepository;

    private final QualityMetricsConfRepository qualityMetricsConfRepository;
    private final QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository;
    private final QualityMetricsConfSourceRepository qualityMetricsConfSourceRepository;
    private final QualityMetricsConfSourcePropertyRepository qualityMetricsConfSourcePropertyRepository;
    private final QualityMetricsConfTestSetThresholdRepository qualityMetricsConfTestSetThresholdRepository;

    public QualityMetricsConfService(
            DataSetRepository dataSetRepository,
            QualityMetricsConfRepository qualityMetricsConfRepository,
            QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository,
            QualityMetricsConfSourceRepository qualityMetricsConfSourceRepository,
            QualityMetricsConfSourcePropertyRepository qualityMetricsConfSourcePropertyRepository, QualityMetricsConfTestSetThresholdRepository qualityMetricsConfTestSetThresholdRepository) {
        this.dataSetRepository = dataSetRepository;
        this.qualityMetricsConfRepository = qualityMetricsConfRepository;
        this.qualityMetricsConfTestSetRepository = qualityMetricsConfTestSetRepository;
        this.qualityMetricsConfSourceRepository = qualityMetricsConfSourceRepository;
        this.qualityMetricsConfSourcePropertyRepository = qualityMetricsConfSourcePropertyRepository;
        this.qualityMetricsConfTestSetThresholdRepository = qualityMetricsConfTestSetThresholdRepository;
    }


    private QualityMetricsConfDTO mapQualityMetricsConfDTO(
            QualityMetricsConf entity,
            Set<QualityMetricsConfTestSet> qualityMetricsConfTestSets,
            Set<QualityMetricsConfTestSetThreshold> thresholds,
            Set<QualityMetricsConfSource> sources){
        QualityMetricsConfDTO result = new QualityMetricsConfDTO();
        result.setKeyName(entity.getKeyName());
        result.setDescription(entity.getDescription());
        result.setEnabled(entity.isEnabled());
        result.setDataSetKeyName(entity.getDataSet().getKeyName());
        result.setQualityMetricsConfTestSets(
                qualityMetricsConfTestSets
                        .stream()
                        .map(this::mapQualityMetricsConfTestSetDTO)
                        .collect(Collectors.toSet()));
        result.setThresholds(
                thresholds
                .stream()
                .map(this::mapQualityMetricsConfTestSetDTO)
                        .collect(Collectors.toSet()));
        result.setSources(
                sources
                .stream()
                .map(e -> {
                    DataSetSourceDTO dto = new DataSetSourceDTO();
                    dto.setName(e.getSource().getKeyName());
                    dto.setProperties(
                            qualityMetricsConfSourcePropertyRepository
                            .findByQualityMetricsConfSourceId(e.getId())
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            QualityMetricsConfSourceProperty::getKey,
                                            QualityMetricsConfSourceProperty::getValue)));
                    return dto;
                }).collect(Collectors.toSet()));

        return result;
    }

    public QualityMetricsConf mapQualityMetricsConf(QualityMetricsConfDTO dto){
        QualityMetricsConf result = new QualityMetricsConf();
        result.setKeyName(dto.getKeyName());
        result.setDescription(dto.getDescription());
        result.setEnabled(dto.isEnabled());
        result.setDataSet(dataSetRepository.getReferenceById(dto.getDataSetKeyName()));
        return result;
    }
    private QualityMetricsConfTestSetAbstract mapQualityMetricsConfTestSetAbstract(
            QualityMetricsConfTestSetDTO dto){
        QualityMetricsConfTestSetAbstract result = new QualityMetricsConfTestSetAbstract();
        result.setDqMetricsType(dto.getDqMetricsType());
        result.setSave(dto.isSave());
        result.setKeyName(dto.getKeyName());
        result.setValue(dto.getValue());
        result.setDescription(dto.getDescription());
        return result;
    }

    public QualityMetricsConfTestSet mapQualityMetricsConfTestSet(
            QualityMetricsConf qualityMetricsConf,
            QualityMetricsConfTestSetDTO dto){
        QualityMetricsConfTestSet result = new QualityMetricsConfTestSet();
        result.of(mapQualityMetricsConfTestSetAbstract(dto));
        result.setQualityMetricsConf(qualityMetricsConf);
        return result;
    }
    public QualityMetricsConfTestSetThreshold mapQualityMetricsConfTestSetThreshold(
            QualityMetricsConf qualityMetricsConf,
            QualityMetricsConfTestSetDTO dto){
        QualityMetricsConfTestSetThreshold result =  new QualityMetricsConfTestSetThreshold();
        result.of(mapQualityMetricsConfTestSetAbstract(dto));
        result.setQualityMetricsConf(qualityMetricsConf);
        return result;
    }
    public QualityMetricsConfTestSetDTO mapQualityMetricsConfTestSetDTO(
            QualityMetricsConfTestSetAbstract entity){
        QualityMetricsConfTestSetDTO result = new QualityMetricsConfTestSetDTO();
        result.setDqMetricsType(entity.getDqMetricsType());
        result.setSave(entity.isSave());
        result.setKeyName(entity.getKeyName());
        result.setValue(entity.getValue());
        result.setDescription(entity.getDescription());
        return result;
    }

    public QualityMetricsConfSource mapQualityMetricsConfSource(QualityMetricsConf qualityMetricsConf,DataSetSourceDTO dto){
        QualityMetricsConfSource result = new QualityMetricsConfSource();
        result.setQualityMetricsConf(qualityMetricsConf);
        result.setSource(dataSetRepository.getReferenceById(dto.getName()));
        return result;
    }

    @Transactional
    public QualityMetricsConfDTO save(QualityMetricsConfDTO qualityMetricsConfDTO){
        QualityMetricsConf qualityMetricsConf = qualityMetricsConfRepository.save(mapQualityMetricsConf(qualityMetricsConfDTO));
        qualityMetricsConfTestSetRepository.deleteByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName());
        qualityMetricsConfTestSetThresholdRepository.deleteByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName());
        qualityMetricsConfSourceRepository.deleteByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName());

        qualityMetricsConfTestSetRepository
                .saveAll(
                        qualityMetricsConfDTO
                                .getQualityMetricsConfTestSets()
                                .stream()
                                .map(o-> mapQualityMetricsConfTestSet(qualityMetricsConf,o)).toList());

        qualityMetricsConfTestSetThresholdRepository
                .saveAll(
                        qualityMetricsConfDTO
                                .getThresholds()
                                .stream()
                                .map(o-> mapQualityMetricsConfTestSetThreshold(qualityMetricsConf,o)).toList());

        qualityMetricsConfDTO
                .getSources()
                .forEach(o-> saveQualityMetricsConfSource(qualityMetricsConf,o));

        return findById(qualityMetricsConfDTO.getKeyName());
    }

    private void  saveQualityMetricsConfSource(
            QualityMetricsConf qualityMetricsConf,
            DataSetSourceDTO dataSetSourceDTO){
        QualityMetricsConfSource source = qualityMetricsConfSourceRepository.save(
                mapQualityMetricsConfSource(qualityMetricsConf,dataSetSourceDTO));
        dataSetSourceDTO.getProperties().entrySet().forEach(stringStringEntry -> {
            QualityMetricsConfSourceProperty dataSetSourceProperty = new QualityMetricsConfSourceProperty();
            dataSetSourceProperty.setQualityMetricsConfSource(source);
            dataSetSourceProperty.setKey(stringStringEntry.getKey());
            dataSetSourceProperty.setValue(stringStringEntry.getValue());
            qualityMetricsConfSourcePropertyRepository.save(dataSetSourceProperty);
        });
    }
    public List<QualityMetricsConfDTO> findAll(){
        return qualityMetricsConfRepository
                .findAll()
                .stream().map(qualityMetricsConf ->
                    mapQualityMetricsConfDTO(
                      qualityMetricsConf,
                      qualityMetricsConfTestSetRepository.findByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName()),
                      qualityMetricsConfTestSetThresholdRepository.findByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName()),
                      qualityMetricsConfSourceRepository.findByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName()))
                        )
                .toList();
    }

    public QualityMetricsConfDTO findById(String name) {
        return mapQualityMetricsConfDTO(
                qualityMetricsConfRepository
                .findByKeyName(name)
                .orElseThrow(() -> new QualityMetricsConfNotFoundException(name)),
                                qualityMetricsConfTestSetRepository.findByQualityMetricsConfKeyName(name),
                                qualityMetricsConfTestSetThresholdRepository.findByQualityMetricsConfKeyName(name),
                                qualityMetricsConfSourceRepository.findByQualityMetricsConfKeyName(name));
    }

    public void deleteById(String name) {
        qualityMetricsConfRepository.deleteById(name);
    }
}

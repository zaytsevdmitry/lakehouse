package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DataSetSourceDTO;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfTestSetDTO;
import org.lakehouse.config.entities.DataSetSource;
import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.lakehouse.config.entities.dq.QualityMetricsConfSource;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.lakehouse.config.repository.DataSetRepository;
import org.lakehouse.config.repository.QualityMetricsConfRepository;
import org.lakehouse.config.repository.QualityMetricsConfSourceRepository;
import org.lakehouse.config.repository.QualityMetricsConfTestSetRepository;
import org.springframework.stereotype.Service;

@Service
public class QualityMetricsConfService {
    private final DataSetRepository dataSetRepository;

    private final QualityMetricsConfRepository qualityMetricsConfRepository;
    private final QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository;
    private final QualityMetricsConfSourceRepository qualityMetricsConfSourceRepository;

    public QualityMetricsConfService(DataSetRepository dataSetRepository, QualityMetricsConfRepository qualityMetricsConfRepository, QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository, QualityMetricsConfSourceRepository qualityMetricsConfSourceRepository) {
        this.dataSetRepository = dataSetRepository;
        this.qualityMetricsConfRepository = qualityMetricsConfRepository;
        this.qualityMetricsConfTestSetRepository = qualityMetricsConfTestSetRepository;
        this.qualityMetricsConfSourceRepository = qualityMetricsConfSourceRepository;
    }

    public QualityMetricsConfDTO mapQualityMetricsConfDTO(QualityMetricsConf entity){
        QualityMetricsConfDTO result = new QualityMetricsConfDTO();
        result.setKeyName(entity.getKeyName());
        result.setDescription(entity.getDescription());
        result.setEnabled(entity.isEnabled());
        result.setDataSetKeyName(entity.getDataSet().getName());
        return result;
    }

    public QualityMetricsConf mapQualityMetricsConf(QualityMetricsConfDTO dto){
        QualityMetricsConf result = new QualityMetricsConf();
        result.setName(dto.getKeyName());
        result.setDescription(dto.getDescription());
        result.setEnabled(dto.isEnabled());
        result.setDataSet(dataSetRepository.getReferenceById(dto.getDataSetKeyName()));
        return result;
    }
    public QualityMetricsConfTestSet mapQualityMetricsConfTestSet(
            QualityMetricsConf qualityMetricsConf,
            QualityMetricsConfTestSetDTO dto){
        QualityMetricsConfTestSet result = new QualityMetricsConfTestSet();
        result.setDqMetricsType(dto.getDqMetricsType());
        result.setSave(dto.isSave());
        result.setKeyName(dto.getKeyName());
        result.setValue(dto.getValue());
        result.setQualityMetricsConf(qualityMetricsConf);
        return result;
    }
    public QualityMetricsConfTestSetDTO mapQualityMetricsConfTestSetDTO(
            QualityMetricsConfTestSet entity){
        QualityMetricsConfTestSetDTO result = new QualityMetricsConfTestSetDTO();
        result.setDqMetricsType(entity.getDqMetricsType());
        result.setSave(entity.isSave());
        result.setKeyName(entity.getKeyName());
        result.setValue(entity.getValue());
        return result;
    }

    public QualityMetricsConfSource mapQualityMetricsConfSource(QualityMetricsConf qualityMetricsConf,DataSetSourceDTO dto){
        QualityMetricsConfSource result = new QualityMetricsConfSource();
        result.setQualityMetricsConf(qualityMetricsConf);
        result.setSource(dataSetRepository.getReferenceById(dto.getName()));
        return result;
    }

    @Transactional
    public void save(QualityMetricsConfDTO qualityMetricsConfDTO){
        QualityMetricsConf qualityMetricsConf = qualityMetricsConfRepository.save(mapQualityMetricsConf(qualityMetricsConfDTO));
        qualityMetricsConfTestSetRepository.deleteByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName());
        qualityMetricsConfSourceRepository.deleteByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName());
        qualityMetricsConfTestSetRepository
                .saveAll(
                        qualityMetricsConfDTO
                                .getQualityMetricsConfTestSets()
                                .stream()
                                .map(o-> mapQualityMetricsConfTestSet(qualityMetricsConf,o)).toList());
        qualityMetricsConfSourceRepository
                .saveAll(
                        qualityMetricsConfDTO
                                .getSources()
                                .stream()
                                .map(o-> mapQualityMetricsConfSource(qualityMetricsConf,o))
                                .toList());

    }
}

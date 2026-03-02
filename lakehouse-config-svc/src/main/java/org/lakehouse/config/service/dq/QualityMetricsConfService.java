package org.lakehouse.config.service.dq;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.config.entities.dq.ElementType;
import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.lakehouse.config.exception.QualityMetricsConfNotFoundException;
import org.lakehouse.config.exception.QualityMetricsConfTestSetNotFoundException;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.config.repository.dq.QualityMetricsConfRepository;
import org.lakehouse.config.repository.dq.QualityMetricsConfTestSetRepository;
import org.lakehouse.config.service.dataset.source.DataSetSourceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QualityMetricsConfService {
    private final DataSetRepository dataSetRepository;

    private final QualityMetricsConfRepository qualityMetricsConfRepository;
    private final QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository;
    private final QualityMetricsConfTestSetService qualityMetricsConfTestSetService;
    private final DataSetSourceService dataSetSourceService;
    public QualityMetricsConfService(
            DataSetRepository dataSetRepository,
            QualityMetricsConfRepository qualityMetricsConfRepository,
            QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository,
            QualityMetricsConfTestSetService qualityMetricsConfTestSetService,
            DataSetSourceService dataSetSourceService) {
        this.dataSetRepository = dataSetRepository;
        this.qualityMetricsConfRepository = qualityMetricsConfRepository;
        this.qualityMetricsConfTestSetRepository = qualityMetricsConfTestSetRepository;
        this.qualityMetricsConfTestSetService = qualityMetricsConfTestSetService;
        this.dataSetSourceService = dataSetSourceService;
    }


    private QualityMetricsConfDTO mapQualityMetricsConfDTO(
            QualityMetricsConf qualityMetricsConf) {
        QualityMetricsConfDTO result = new QualityMetricsConfDTO();

        result.setDqThresholdViolationLevel(qualityMetricsConf.getDqThresholdViolationLevel());
        result.setKeyName(qualityMetricsConf.getKeyName());
        result.setDescription(qualityMetricsConf.getDescription());
        result.setEnabled(qualityMetricsConf.isEnabled());
        result.setDataSetKeyName(qualityMetricsConf.getDataSet().getKeyName());
        result.setTestSets(
                qualityMetricsConfTestSetRepository
                        .findByQualityMetricsConfKeyNameAndElementType(qualityMetricsConf.getKeyName(),ElementType.TEST_SET)
                        .stream()
                        .map(qualityMetricsConfTestSetService::mapQualityMetricsConfTestSetDTO)
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))
        );
        result.setThresholds(
                qualityMetricsConfTestSetRepository
                        .findByQualityMetricsConfKeyNameAndElementType(qualityMetricsConf.getKeyName(),ElementType.THRESHOLD)
                        .stream()
                        .map(qualityMetricsConfTestSetService::mapQualityMetricsConfTestSetDTO)
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))
        );
        result.setMetric(
                qualityMetricsConfTestSetRepository
                .findByQualityMetricsConfKeyNameAndElementType(
                        qualityMetricsConf.getKeyName(),
                        ElementType.METRIC)
                        .stream()
                        .map(qualityMetricsConfTestSetService::mapQualityMetricsConfTestSetDTO)
                        .findFirst()
                        .orElseThrow(() -> new QualityMetricsConfTestSetNotFoundException("Test set with type metric not found"))
                        .getValue()
        );
        result.setSources(
                dataSetSourceService
                        .findByQualityMetricsConfKeyName(qualityMetricsConf.getKeyName()));

        return result;
    }

    public QualityMetricsConf mapQualityMetricsConf(QualityMetricsConfDTO dto) {
        QualityMetricsConf result = new QualityMetricsConf();
        result.setKeyName(dto.getKeyName());
        result.setDescription(dto.getDescription());
        result.setEnabled(dto.isEnabled());
        result.setDataSet(dataSetRepository.getReferenceById(dto.getDataSetKeyName()));
        result.setDqThresholdViolationLevel(dto.getDqThresholdViolationLevel());
        return result;
    }

    private QualityMetricsConfTestSet mapQualityMetricsConfTestSetAbstract(
            Map.Entry<String, QualityMetricsConfTestSetDTO> dto) {
        QualityMetricsConfTestSet result = new QualityMetricsConfTestSet();
        result.setDqMetricsType(dto.getValue().getType());
        result.setSave(dto.getValue().isSave());
        result.setKeyName(dto.getKey());
        result.setDescription(dto.getValue().getDescription());
        return result;
    }

    public QualityMetricsConfTestSet mapQualityMetricsConfTestSet(
            QualityMetricsConf qualityMetricsConf,
            Map.Entry<String, QualityMetricsConfTestSetDTO> dto) {
        QualityMetricsConfTestSet result = new QualityMetricsConfTestSet();
        result.of(mapQualityMetricsConfTestSetAbstract(dto));
        result.setQualityMetricsConf(qualityMetricsConf);
        return result;
    }

    @Transactional
    public QualityMetricsConfDTO save(QualityMetricsConfDTO qualityMetricsConfDTO) {
        QualityMetricsConf qualityMetricsConf = qualityMetricsConfRepository.save(mapQualityMetricsConf(qualityMetricsConfDTO));

        qualityMetricsConfTestSetService.save(qualityMetricsConf,qualityMetricsConfDTO.getTestSets(), ElementType.TEST_SET);
        qualityMetricsConfTestSetService.save(qualityMetricsConf,qualityMetricsConfDTO.getThresholds(), ElementType.THRESHOLD);
        qualityMetricsConfTestSetService.save(qualityMetricsConf,Map.of(qualityMetricsConf.getKeyName(), qualityMetricsConfDTO.getMetric()), ElementType.METRIC);
        dataSetSourceService.save(qualityMetricsConf, qualityMetricsConfDTO.getSources());
        return findById(qualityMetricsConfDTO.getKeyName());
    }


    public List<QualityMetricsConfDTO> findAll() {
        return qualityMetricsConfRepository
                .findAll()
                .stream().map(this::mapQualityMetricsConfDTO)
                .toList();
    }

    public QualityMetricsConfDTO findById(String name) {
        return mapQualityMetricsConfDTO(
                qualityMetricsConfRepository
                        .findByKeyName(name)
                        .orElseThrow(() -> new QualityMetricsConfNotFoundException(name)));
    }

    public void deleteById(String name) {
        qualityMetricsConfRepository.deleteById(name);
    }

    public List<QualityMetricsConfDTO> findByDataSetKeyName(String dataSetKeyName) {
        return qualityMetricsConfRepository
                .findByDataSetKeyName(dataSetKeyName)
                .stream()
                .map(this::mapQualityMetricsConfDTO).toList();
    }
}

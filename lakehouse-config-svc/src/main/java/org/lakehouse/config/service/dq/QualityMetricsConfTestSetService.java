package org.lakehouse.config.service.dq;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.lakehouse.config.repository.dq.QualityMetricsConfTestSetRepository;
import org.lakehouse.config.service.ScriptReferenceService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class QualityMetricsConfTestSetService {
    private final QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository;
    private final ScriptReferenceService scriptReferenceService;

    public QualityMetricsConfTestSetService(QualityMetricsConfTestSetRepository qualityMetricsConfTestSetRepository,
                                            ScriptReferenceService scriptReferenceService) {
        this.qualityMetricsConfTestSetRepository = qualityMetricsConfTestSetRepository;
        this.scriptReferenceService = scriptReferenceService;

    }

    private QualityMetricsConfTestSet mapTestSet(
            QualityMetricsConf qualityMetricsConf,
            Map.Entry<String, QualityMetricsConfTestSetDTO> dto,
            boolean isThreshold){
        QualityMetricsConfTestSet result = qualityMetricsConfTestSetRepository
                .findByQualityMetricsConfKeyNameAndKeyName(qualityMetricsConf.getKeyName(),dto.getKey())
                .orElse( new QualityMetricsConfTestSet());
        result.setDescription(dto.getValue().getDescription());
        result.setQualityMetricsConf(qualityMetricsConf);
        result.setDqMetricsType(dto.getValue().getDqMetricsType());
        result.setSave(dto.getValue().isSave());
        result.setKeyName(dto.getKey());
        result.setThreshold(isThreshold);
        return result;
    }

    @Transactional
    public void save(
            QualityMetricsConf qualityMetricsConf,
            Map<String, QualityMetricsConfTestSetDTO> qualityMetricsConfTestSetDTOs,
            boolean isThreshold){

        qualityMetricsConfTestSetRepository.deleteAll(
                qualityMetricsConfTestSetRepository
                .findByQualityMetricsConfKeyNameAndIsThreshold(qualityMetricsConf.getKeyName(),isThreshold)
                .stream()
                .filter(q-> !qualityMetricsConfTestSetDTOs.containsKey(q.getKeyName()))
                .toList());



        for (Map.Entry<String, QualityMetricsConfTestSetDTO> entry: qualityMetricsConfTestSetDTOs.entrySet()){
            QualityMetricsConfTestSet testSet = qualityMetricsConfTestSetRepository.save(mapTestSet(qualityMetricsConf, entry, isThreshold));
            scriptReferenceService.saveQualityTestSet(testSet, entry.getValue().getScripts());

        }
    }

    public Map.Entry<String,QualityMetricsConfTestSetDTO> mapQualityMetricsConfTestSetDTO(
            QualityMetricsConfTestSet entity) {
        QualityMetricsConfTestSetDTO result = new QualityMetricsConfTestSetDTO();
        result.setDqMetricsType(entity.getDqMetricsType());
        result.setSave(entity.isSave());
        result.setDescription(entity.getDescription());
        result.setScripts( scriptReferenceService.findByQualityMetricsConfTestSetKeyNameOrderByScriptOrder(entity.getKeyName()));
        return Map.entry(entity.getKeyName(),result);
    }
}

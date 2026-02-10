package org.lakehouse.config.service;

import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.ScriptReference;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.lakehouse.config.repository.ScriptReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScriptReferenceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScriptReferenceRepository scriptReferenceRepository;
    private final ScriptService scriptService;
    public ScriptReferenceService(
            ScriptReferenceRepository scriptReferenceRepository, ScriptService scriptService) {
        this.scriptReferenceRepository = scriptReferenceRepository;
        this.scriptService = scriptService;
    }

    /**
     * @param dataSetName
     * @return Note: @Transactional used by cause // Caused by: org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
     */
    @Transactional
    public List<ScriptReferenceDTO> findDataSetScriptDTOListByDataSetName(String dataSetName) {

        return scriptReferenceRepository
                .findByDataSetKeyNameOrderByScriptOrder(dataSetName)
                .stream().map(this::mapScriptReferenceDTO)
                .toList();
    }

    @Transactional
    public List<ScriptReferenceDTO> findByQualityMetricsConfTestSetKeyNameOrderByScriptOrder(String testConf){
        return scriptReferenceRepository
                .findByQualityMetricsConfTestSetKeyNameOrderByScriptOrder(testConf)
                .stream().map(this::mapScriptReferenceDTO)
                .toList();
    }

    private ScriptReferenceDTO mapScriptReferenceDTO(ScriptReference scriptReference){
        ScriptReferenceDTO scriptReferenceDTO = new ScriptReferenceDTO();
        scriptReferenceDTO.setKey(scriptReference.getScript().getKey());
        scriptReferenceDTO.setOrder(scriptReference.getScriptOrder());
        return scriptReferenceDTO;
    }

    @Transactional
    public List<ScriptReference> findDataSetScriptListByDataSetName(String dataSetName) {
        return scriptReferenceRepository.findByDataSetKeyNameOrderByScriptOrder(dataSetName);
    }

    @Transactional
    public void saveDataSetScript(DataSet dataSet, List<ScriptReferenceDTO> scriptReferenceDTOs){
        save(
                dataSet,
                null,
                scriptReferenceDTOs,
                scriptReferenceRepository.findByDataSetKeyNameOrderByScriptOrder(dataSet.getKeyName()));
    }

    @Transactional
    public void saveQualityTestSet(QualityMetricsConfTestSet qualityMetricsConfTestSet, List<ScriptReferenceDTO> scriptReferenceDTOs){
        save(
                null,
                qualityMetricsConfTestSet,
                scriptReferenceDTOs,
                scriptReferenceRepository.findByDataSetKeyNameOrderByScriptOrder(qualityMetricsConfTestSet.getKeyName()));
    }

    @Transactional
    private void save(
            DataSet dataSet,
            QualityMetricsConfTestSet qualityMetricsConfTestSet,
            List<ScriptReferenceDTO> scriptReferenceDTOsNew,
            List<ScriptReference> scriptReferencesCurrent
    ){
        Set<String> keysNew = scriptReferenceDTOsNew.stream().map(ScriptReferenceDTO::getKey).collect(Collectors.toSet());
        Map<String, ScriptReference> keysCur =
                scriptReferencesCurrent
                        .stream()
                        .map(r ->  Map.entry(r.getScript().getKey(),r))
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));


        scriptReferenceRepository.deleteAll(
                scriptReferencesCurrent
                        .stream()
                        .filter(s-> !keysNew.contains(s.getScript().getKey()))
                        .toList());


        scriptReferenceRepository.saveAll(
                scriptReferenceDTOsNew
                        .stream()
                        .map(dataSetScriptDTO -> {
                            ScriptReference scriptReference;
                            if (keysCur.containsKey(dataSetScriptDTO.getKey())) {
                                scriptReference = keysCur.get(dataSetScriptDTO.getKey());
                            }
                            else {
                                scriptReference = new ScriptReference();
                            }
                            scriptReference.setDataSet(dataSet);
                            scriptReference.setQualityMetricsConfTestSet(qualityMetricsConfTestSet);
                            scriptReference.setScript(scriptService.findScriptByKey(dataSetScriptDTO.getKey()));
                            scriptReference.setScriptOrder(dataSetScriptDTO.getOrder());
                            return scriptReference;})
                        .toList());
    }
}

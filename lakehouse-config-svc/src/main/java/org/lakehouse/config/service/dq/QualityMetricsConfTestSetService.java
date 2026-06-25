/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.config.service.dq;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfTestSetDTO;
import org.lakehouse.config.entities.dq.ElementType;
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
            ElementType elementType){
        QualityMetricsConfTestSet result = qualityMetricsConfTestSetRepository
                .findByQualityMetricsConfKeyNameAndKeyName(qualityMetricsConf.getKeyName(),dto.getKey())
                .orElse( new QualityMetricsConfTestSet());
        result.setDescription(dto.getValue().getDescription());
        result.setQualityMetricsConf(qualityMetricsConf);
        result.setDqMetricsType(dto.getValue().getType());
        result.setKeyName(dto.getKey());
        result.setElementType(elementType);
        return result;
    }

    @Transactional
    public void save(
            QualityMetricsConf qualityMetricsConf,
            Map<String, QualityMetricsConfTestSetDTO> qualityMetricsConfTestSetDTOs,
            ElementType elementType){

        qualityMetricsConfTestSetRepository.deleteAll(
                qualityMetricsConfTestSetRepository
                .findByQualityMetricsConfKeyNameAndElementType(qualityMetricsConf.getKeyName(),elementType)
                .stream()
                .filter(q-> !qualityMetricsConfTestSetDTOs.containsKey(q.getKeyName()))
                .toList());



        for (Map.Entry<String, QualityMetricsConfTestSetDTO> entry: qualityMetricsConfTestSetDTOs.entrySet()){
            QualityMetricsConfTestSet testSet = qualityMetricsConfTestSetRepository.save(mapTestSet(qualityMetricsConf, entry, elementType));
            scriptReferenceService.saveQualityTestSet(testSet, entry.getValue().getScripts());

        }
    }

    public Map.Entry<String,QualityMetricsConfTestSetDTO> mapQualityMetricsConfTestSetDTO(
            QualityMetricsConfTestSet entity) {
        QualityMetricsConfTestSetDTO result = new QualityMetricsConfTestSetDTO();
        result.setType(entity.getDqMetricsType());
        result.setDescription(entity.getDescription());
        result.setScripts( scriptReferenceService.findByQualityMetricsConfTestSetIdOrderByScriptOrder(entity.getId()));
        return Map.entry(entity.getKeyName(),result);
    }
}

/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

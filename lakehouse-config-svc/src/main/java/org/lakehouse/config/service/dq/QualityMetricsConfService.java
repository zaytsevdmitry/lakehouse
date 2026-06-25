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
        result.setSave(qualityMetricsConf.isSave());
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
                .findByQualityMetricsConfKeyNameAndElementType(qualityMetricsConf.getKeyName(), ElementType.METRIC)
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
        result.setSave(dto.isSave());
        result.setDataSet(dataSetRepository.getReferenceById(dto.getDataSetKeyName()));
        result.setDqThresholdViolationLevel(dto.getDqThresholdViolationLevel());
        return result;
    }

    private QualityMetricsConfTestSet mapQualityMetricsConfTestSetAbstract(
            Map.Entry<String, QualityMetricsConfTestSetDTO> dto) {
        QualityMetricsConfTestSet result = new QualityMetricsConfTestSet();
        result.setDqMetricsType(dto.getValue().getType());

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

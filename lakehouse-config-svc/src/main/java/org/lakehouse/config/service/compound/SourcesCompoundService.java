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

package org.lakehouse.config.service.compound;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.config.repository.dq.QualityMetricsConfRepository;
import org.lakehouse.config.service.dataset.DataSetService;
import org.lakehouse.config.service.datasource.DataSourceService;
import org.lakehouse.config.service.datasource.DriverService;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.jinja.java.util.SourceConfUtil;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SourcesCompoundService {
    private final DataSetService dataSetService;
    private final QualityMetricsConfRepository qualityMetricsConfRepository;
    private final DataSourceService dataSourceService;
    private final DriverService driverService;
    private final JinJavaUtils jinJavaUtils;
    public SourcesCompoundService(
            DataSetService dataSetService,
            QualityMetricsConfRepository qualityMetricsConfRepository,
            DataSourceService dataSourceService,
            DriverService driverService,
            JinJavaUtils jinJavaUtils) {
        this.dataSetService = dataSetService;
        this.qualityMetricsConfRepository = qualityMetricsConfRepository;
        this.dataSourceService = dataSourceService;
        this.driverService = driverService;
        this.jinJavaUtils = jinJavaUtils;
    }

    public SourceConfDTO getSourceConfDTO(String dataSetKeyName){
        SourceConfDTO result = new SourceConfDTO();
        result.setTargetDataSetKeyName(dataSetKeyName);
        DataSetDTO  dataSetDTO = dataSetService.findById(dataSetKeyName);
        result.setDataSets(collapseDataSetDTOs(dataSetDTO));
        result.setDataSources(
                result.getDataSets()
                        .values().stream()
                        .map(DataSetDTO::getDataSourceKeyName)
                        .collect(Collectors.toSet())
                        .stream()
                        .map(dataSourceService::findById)
                        .collect(Collectors.toMap(DataSourceDTO::getKeyName, Function.identity()))
        );
        result.setDrivers(
                result.getDataSources()
                        .values()
                        .stream()
                        .map(DataSourceDTO::getDriverKeyName)
                        .collect(Collectors.toSet())
                        .stream()
                        .map(driverService::findById)
                        .collect(Collectors.toMap(DriverDTO::getKeyName,Function.identity()))
        );

        new SourceConfUtil(jinJavaUtils).renderProperties(result);
        return result;
    }

    private Map<String,DataSetDTO> collapseDataSetDTOs(DataSetDTO  dataSetDTO) {
        Map<String,DataSetDTO> result = new HashMap<>();
        Set<String> dataSetKeyNames = new HashSet<>();


        dataSetKeyNames.addAll(
                dataSetDTO.getConstraints()
                        .values()
                        .stream()
                        .filter(c-> c.getType().equals(Types.Constraint.foreign))
                        .map(c -> c.getReference().getDataSetKeyName())
                        .toList());

        dataSetKeyNames.addAll(
                dataSetDTO
                        .getSources().keySet());

        dataSetKeyNames.addAll(
        qualityMetricsConfRepository
                .findByDataSetKeyName(dataSetDTO.getKeyName())
                .stream()
                .map(qualityMetricsConf -> qualityMetricsConf.getDataSet().getKeyName()).collect(Collectors.toSet()));

        result.put(dataSetDTO.getKeyName(),dataSetDTO);
        result.putAll(dataSetKeyNames.stream()
                .map(dataSetService::findById)
                .collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity())));
        return result;
    }


}

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

package org.lakehouse.config.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.config.service.dq.QualityMetricsConfService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QualityMetricsConfController {
    private final QualityMetricsConfService qualityMetricsConfService;

    public QualityMetricsConfController(QualityMetricsConfService qualityMetricsConfService) {
        this.qualityMetricsConfService = qualityMetricsConfService;
    }

    @GetMapping(Endpoint.QUALITY_METRICS)
    List<QualityMetricsConfDTO> getAll() {
        return qualityMetricsConfService.findAll();
    }

    @PostMapping(Endpoint.QUALITY_METRICS)
    @ResponseStatus(HttpStatus.CREATED)
    void post(@RequestBody QualityMetricsConfDTO qualityMetricsConfDTO) {
        qualityMetricsConfService.save(qualityMetricsConfDTO);
    }

    @GetMapping(Endpoint.QUALITY_METRICS_NAME)
    @ResponseStatus(HttpStatus.OK)
    QualityMetricsConfDTO get(@PathVariable String keyName) {
        return qualityMetricsConfService.findById(keyName);
    }
    @GetMapping(Endpoint.QUALITY_METRICS_BY_DATASET)
    @ResponseStatus(HttpStatus.OK)
    List<QualityMetricsConfDTO>  getByDataSetKeyName(@PathVariable String keyName) {
        return qualityMetricsConfService.findByDataSetKeyName(keyName);
    }

    @DeleteMapping(Endpoint.QUALITY_METRICS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        qualityMetricsConfService.deleteById(keyName);
    }
}

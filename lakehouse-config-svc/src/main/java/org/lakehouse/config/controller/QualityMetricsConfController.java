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

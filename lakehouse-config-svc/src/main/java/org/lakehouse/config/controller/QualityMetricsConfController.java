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
    QualityMetricsConfDTO get(@PathVariable String name) {
        return qualityMetricsConfService.findById(name);
    }

    @DeleteMapping(Endpoint.QUALITY_METRICS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        qualityMetricsConfService.deleteById(name);
    }
}

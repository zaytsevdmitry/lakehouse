package org.lakehouse.config.controller.compound;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.config.service.compound.SourcesCompoundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class SourceConfController {
    private final SourcesCompoundService sourcesCompoundService;

    public SourceConfController(SourcesCompoundService sourcesCompoundService) {
        this.sourcesCompoundService = sourcesCompoundService;
    }

    @GetMapping(Endpoint.SOURCES_CONF_BY_DATASET_KEY_NAME)
    public SourceConfDTO getSourceConfDTO(@PathVariable String dataSetKeyName){
        return sourcesCompoundService.getSourceConfDTO(dataSetKeyName);
    }
}

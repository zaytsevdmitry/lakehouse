package org.lakehouse.config.controller.compound;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.config.service.compound.ScriptCompoundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSetModelScriptController {
    private final ScriptCompoundService scriptCompoundService;

    public DataSetModelScriptController(ScriptCompoundService scriptCompoundService) {
        this.scriptCompoundService = scriptCompoundService;
    }

    @GetMapping(Endpoint.DATASET_MODEL_SCRIPT_BY_DATASET_KEY_NAME)
    public String getModelScript(@PathVariable String dataSetKeyName){
       return scriptCompoundService.getCompoundDataSetScript(dataSetKeyName);
    }
}

package org.lakehouse.config.service.compound;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.config.service.ScriptReferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ScriptCompoundService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScriptReferenceService scriptReferenceService;
    public ScriptCompoundService(ScriptReferenceService scriptReferenceService) {
        this.scriptReferenceService = scriptReferenceService;
    }

    public String getCompoundDataSetScript(String dataSetKeyName){
        return scriptReferenceService
                .findDataSetScriptListByDataSetName(dataSetKeyName)
                .stream()
                .map(dss -> dss.getScript().getValue())
                .collect(Collectors.joining(SystemVarKeys.SCRIPT_DELIMITER));

    }
}

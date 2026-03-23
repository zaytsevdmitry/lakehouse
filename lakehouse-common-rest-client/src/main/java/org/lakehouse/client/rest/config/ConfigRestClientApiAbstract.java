package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.client.rest.exception.ScriptBuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public abstract class ConfigRestClientApiAbstract implements ConfigRestClientApi {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Override
    public String getScriptByListOfReference(List<ScriptReferenceDTO> scriptReferences) throws ScriptBuildException {
        List<String> scripts = new ArrayList<>();
        scriptReferences.forEach(scriptReferenceDTO -> logger.info(scriptReferenceDTO.toString()));

        List<ScriptReferenceDTO> orderedScriptReferences = new ArrayList<>();
        orderedScriptReferences.addAll(
                scriptReferences
                        .stream()
                        .filter(scriptReferenceDTO -> scriptReferenceDTO.getOrder() != null)
                        .sorted(Comparator.comparingInt(ScriptReferenceDTO::getOrder))
                        .toList());
        orderedScriptReferences.addAll(
                scriptReferences
                        .stream()
                        .filter(scriptReferenceDTO -> scriptReferenceDTO.getOrder() == null)
                        .toList());


        for (ScriptReferenceDTO sr: orderedScriptReferences){
            String script = getScript(sr.getKey());
            if (script == null ||script.isBlank()){
                throw new ScriptBuildException("Empty script");
            }else
                scripts.add(script);
        }

        String result = scripts.stream().collect(Collectors.joining(SystemVarKeys.SCRIPT_DELIMITER));

        return result;
    }
}

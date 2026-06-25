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

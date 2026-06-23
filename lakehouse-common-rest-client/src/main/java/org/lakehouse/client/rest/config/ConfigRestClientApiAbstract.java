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

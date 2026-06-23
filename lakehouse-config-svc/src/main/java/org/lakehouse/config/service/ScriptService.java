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

package org.lakehouse.config.service;

import org.lakehouse.config.entities.script.Script;
import org.lakehouse.config.exception.ScriptNotFoundException;
import org.lakehouse.config.repository.ScriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScriptService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScriptRepository scriptRepository;

    public ScriptService(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    public Map<String, String> findAll() {
        return scriptRepository.findAll().stream().collect(Collectors.toMap(Script::getKey, Script::getValue));
    }


    public String findScriptBodyByKey(String key) {
        return findScriptByKey(key).getValue();
    }

    public Script findScriptByKey(String key) {
        return scriptRepository.findById(key).orElseThrow(() -> {
            logger.info("Script {} not found", key);
            scriptRepository.findAll().forEach(script -> logger.info("Found script key={}", script.getKey()));
            return new ScriptNotFoundException(key);
        });
    }

    public String save(String key, String value) {
        Script script = new Script();
        script.setKey(key);
        script.setValue(value);
        return scriptRepository.save(script).getValue();
    }

    public void deleteById(String key) {
        scriptRepository.deleteById(key);
    }
}

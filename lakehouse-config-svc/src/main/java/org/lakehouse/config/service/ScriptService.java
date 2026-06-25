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

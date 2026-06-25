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
import org.lakehouse.config.service.ScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ScriptController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScriptService scriptService;

    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @GetMapping(Endpoint.SCRIPTS)
    Map<String, String> getAll() {
        Map<String, String> result = scriptService.findAll();
        result.forEach((k, v) -> logger.info("===={}====\n{}\n", k, v));
        return result;
    }

    @PostMapping(Endpoint.SCRIPT_BY_KEY)
    @ResponseStatus(HttpStatus.CREATED)
    String post(@PathVariable String key, @RequestBody String value) {
        return scriptService.save(key, value);
    }

    @GetMapping(Endpoint.SCRIPT_BY_KEY)
    String get(@PathVariable String key) {
        return scriptService.findScriptBodyByKey(key);
    }

    @DeleteMapping(Endpoint.SCRIPT_BY_KEY)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        scriptService.deleteById(name);
    }
}

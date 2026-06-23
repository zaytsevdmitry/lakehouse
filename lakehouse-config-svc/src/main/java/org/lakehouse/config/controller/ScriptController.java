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

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
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.config.service.NameSpaceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class NameSpaceController {
    private final NameSpaceService nameSpaceService;

    public NameSpaceController(NameSpaceService nameSpaceService) {
        this.nameSpaceService = nameSpaceService;
    }

    @GetMapping(Endpoint.NAME_SPACES)
    List<NameSpaceDTO> findAll() {
        return nameSpaceService.getFindAll();
    }

    @PostMapping(Endpoint.NAME_SPACES)
    @ResponseStatus(HttpStatus.CREATED)
    NameSpaceDTO put(@RequestBody NameSpaceDTO nameSpaceDTO) {
        return nameSpaceService.save(nameSpaceDTO);
    }

    @GetMapping(Endpoint.NAME_SPACES_NAME)
    NameSpaceDTO get(@PathVariable String keyName) {
        return nameSpaceService.findByName(keyName);
    }

    @DeleteMapping(Endpoint.NAME_SPACES_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        nameSpaceService.deleteById(keyName);
    }
}

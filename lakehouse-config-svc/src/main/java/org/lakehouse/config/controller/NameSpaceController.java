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

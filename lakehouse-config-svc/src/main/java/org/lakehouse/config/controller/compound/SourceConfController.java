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

package org.lakehouse.config.controller.compound;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.config.service.compound.SourcesCompoundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class SourceConfController {
    private final SourcesCompoundService sourcesCompoundService;

    public SourceConfController(SourcesCompoundService sourcesCompoundService) {
        this.sourcesCompoundService = sourcesCompoundService;
    }

    @GetMapping(Endpoint.SOURCES_CONF_BY_DATASET_KEY_NAME)
    public SourceConfDTO getSourceConfDTO(@PathVariable String keyName){
        return sourcesCompoundService.getSourceConfDTO(keyName);
    }
}

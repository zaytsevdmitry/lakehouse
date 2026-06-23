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

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.config.entities.NameSpace;
import org.lakehouse.config.exception.NameSpaceNotFoundException;
import org.lakehouse.config.repository.NameSpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NameSpaceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final NameSpaceRepository nameSpaceRepository;

    public NameSpaceService(NameSpaceRepository nameSpaceRepository) {
        this.nameSpaceRepository = nameSpaceRepository;
    }

    private NameSpaceDTO mapToDTO(NameSpace nameSpace) {
        NameSpaceDTO result = new NameSpaceDTO();
        result.setKeyName(nameSpace.getKeyName());
        result.setDescription(nameSpace.getDescription());
        return result;
    }

    private NameSpace mapToEntity(NameSpaceDTO nameSpaceDTO) {
        NameSpace result = new NameSpace();
        result.setKeyName(nameSpaceDTO.getKeyName());
        result.setDescription(nameSpaceDTO.getDescription());
        return result;
    }

    public List<NameSpaceDTO> getFindAll() {
        return nameSpaceRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public NameSpaceDTO save(NameSpaceDTO nameSpaceDTO) {
        return mapToDTO(nameSpaceRepository.save(mapToEntity(nameSpaceDTO)));
    }

    public NameSpaceDTO findByName(String name) {
        return mapToDTO(nameSpaceRepository.findById(name).orElseThrow(() -> {
            logger.info("Can't get name: {}", name);
            return new NameSpaceNotFoundException(name);
        }));
    }

    @Transactional
    public void deleteById(String name) {
        nameSpaceRepository.deleteById(name);
    }
}

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

package org.lakehouse.config.service.datasource;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.schedule.DriverDTO;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.exception.VcsManagedException;
import org.lakehouse.config.exception.DriverNotFoundException;
import org.lakehouse.config.repository.datasource.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class DriverService {
    private final DriverRepository driverRepository;
    private final SQLTemplateService sqlTemplateService;

    public DriverService(
            DriverRepository driverRepository,
            SQLTemplateService sqlTemplateService) {
        this.driverRepository = driverRepository;
        this.sqlTemplateService = sqlTemplateService;

    }
    private Driver mapToEntity(DriverDTO driverDTO){
        Driver result = new Driver();
        result.setDescription(driverDTO.getDescription());
        result.setKeyName(driverDTO.getKeyName());
        return result;
    }
    public DriverDTO mapToDTO(Driver driver){
        DriverDTO result = new DriverDTO();
        result.setDescription(driver.getDescription());
        result.setKeyName(driver.getKeyName());
        result.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(driver));
        return result;
    }
    @Transactional
    public DriverDTO save(DriverDTO driverDTO){
        rejectIfVcsManaged(driverDTO.getKeyName(), "created or updated");
        return saveInternal(driverDTO, false);
    }

    @Transactional
    public DriverDTO saveVcs(DriverDTO driverDTO){
        return saveInternal(driverDTO, true);
    }

    private DriverDTO saveInternal(DriverDTO driverDTO, boolean vcsManaged){
        Driver driver = driverRepository.save(mapToEntity(driverDTO));
        driver.setVcsManaged(vcsManaged);
        driverRepository.save(driver);
        sqlTemplateService.save(driver,driverDTO.getSqlTemplate());
        sqlTemplateService.markDriverManaged(driver, vcsManaged);
        return mapToDTO(driver);
    }

    public List<DriverDTO> findAll() {
        return driverRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    public DriverDTO findById(String name) {
        return mapToDTO(findDriverById(name));
    }
    public Driver findDriverById(String name){
        if (name == null|| name.isBlank())
            throw new DriverNotFoundException("Driver key is empty");
        return driverRepository
                .findById(name)
                .orElseThrow(() -> new DriverNotFoundException(String.format("Driver with key %s not found. Load driver before use it", name)));
    }

    public void deleteById(String name) {
        rejectIfVcsManaged(name, "deleted");
        driverRepository.deleteById(name);
    }

    public void unmanage(String name) {
        driverRepository.findById(name).ifPresent(driver -> {
            driver.setVcsManaged(false);
            driverRepository.save(driver);
            sqlTemplateService.markDriverManaged(driver, false);
        });
    }

    private void rejectIfVcsManaged(String name, String operation) {
        driverRepository.findById(name)
                .filter(Driver::isVcsManaged)
                .ifPresent(driver -> {
                    throw new VcsManagedException(name, operation);
                });
    }
}

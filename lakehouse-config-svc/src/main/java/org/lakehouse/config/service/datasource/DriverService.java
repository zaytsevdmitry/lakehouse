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

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.config.entities.datasource.ConnectionTemplate;
import org.lakehouse.config.entities.datasource.Driver;
import org.lakehouse.config.exception.DriverNotFoundException;
import org.lakehouse.config.repository.datasource.ConnectionTemplateRepository;
import org.lakehouse.config.repository.datasource.DriverRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class DriverService {
    private final DriverRepository driverRepository;
    private final ConnectionTemplateRepository connectionTemplateRepository;
    private final SQLTemplateService sqlTemplateService;

    public DriverService(
            DriverRepository driverRepository,
            ConnectionTemplateRepository connectionTemplateRepository,
            SQLTemplateService sqlTemplateService) {
        this.driverRepository = driverRepository;
        this.connectionTemplateRepository = connectionTemplateRepository;
        this.sqlTemplateService = sqlTemplateService;

    }
    private Driver mapToEntity(DriverDTO driverDTO){
        Driver result = new Driver();
        result.setDescription(driverDTO.getDescription());
        result.setKeyName(driverDTO.getKeyName());
        result.setDataSourceType(driverDTO.getDataSourceType());
        return result;
    }
    public DriverDTO mapToDTO(Driver driver){
        DriverDTO result = new DriverDTO();
        result.setDescription(driver.getDescription());
        result.setKeyName(driver.getKeyName());
        result.setConnectionTemplates(
                connectionTemplateRepository
                        .findByDriverKeyName(driver.getKeyName())
                        .stream()
                        .map(connectionTemplate ->
                            Map.entry(Types.ConnectionType.valueOf(connectionTemplate.getKey()),connectionTemplate.getValue())
                        )
                        .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue))
        );
        result.setDataSourceType(driver.getDataSourceType());
        result.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(driver));
        return result;
    }
    public DriverDTO save(DriverDTO driverDTO){
        Driver driver = driverRepository.save(mapToEntity(driverDTO));

        sqlTemplateService.save(driver,driverDTO.getSqlTemplate());

        connectionTemplateRepository.deleteAll(connectionTemplateRepository.findByDriverKeyName(driver.getKeyName()));
        connectionTemplateRepository
                .saveAll(
                    driverDTO
                            .getConnectionTemplates()
                            .entrySet()
                            .stream().map(e-> {
                                ConnectionTemplate template = new ConnectionTemplate();
                                template.setDriver(driver);
                                template.setKey(e.getKey().label);
                                template.setValue(e.getValue());
                                return template;})
                            .toList());
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
            throw new DriverNotFoundException("");
        return driverRepository
                .findById(name)
                .orElseThrow(() -> new DriverNotFoundException(name));
    }

    public void deleteById(String name) {
        driverRepository.deleteById(name);
    }
}

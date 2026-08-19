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

package org.lakehouse.client.api.dto.configs.schedule;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;

import java.util.Objects;

public class DriverDTO {
    private String keyName;
    private String description;
    private SQLTemplateDTO sqlTemplate;
    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SQLTemplateDTO getSqlTemplate() {
        return sqlTemplate;
    }

    public void setSqlTemplate(SQLTemplateDTO sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }


    @Override
    public String toString() {
        return "DriverDTO{" +
                "keyName='" + keyName + '\'' +
                ", description='" + description + '\'' +
                ", sqlTemplate=" + sqlTemplate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DriverDTO driverDTO = (DriverDTO) o;
        return Objects.equals(getKeyName(), driverDTO.getKeyName()) && Objects.equals(getDescription(), driverDTO.getDescription()) && Objects.equals(getSqlTemplate(), driverDTO.getSqlTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getDescription(), getSqlTemplate());
    }
}

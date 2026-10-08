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

package org.lakehouse.config.entities;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class TaskExecutionServiceGroup extends KeyEntityAbstract {

    @Column(nullable = false)
    private boolean isVcsManaged;

    @Column
    private String domainKeyName;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "task_execution_service_group_domains", // Имя отдельной таблицы для хранения списка
            joinColumns = @JoinColumn(name = "tesg_keyName")   // Внешний ключ, связывающий с основной таблицей
    )
    @Column(name = "domain_name") // Имя колонки для самого значения String в новой таблице
    private List<String> allowedDomains = new ArrayList<>();

    public TaskExecutionServiceGroup() {
    }

    public boolean isVcsManaged() {
        return isVcsManaged;
    }

    public void setVcsManaged(boolean vcsManaged) {
        this.isVcsManaged = vcsManaged;
    }

    public List<String> getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(List<String> allowedDomains) {
        this.allowedDomains = allowedDomains;
    }

    public String getDomainKeyName() {
        return domainKeyName;
    }

    public void setDomainKeyName(String domainKeyName) {
        this.domainKeyName = domainKeyName;
    }
}

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

package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskTemplateRepository extends JpaRepository<TemplateTask, Long> {

    List<TemplateTask> findByTemplateScenarioActKeyName(String scenarioActTemplateName);

    //@Query("select p from TemplateTask p where p.templateScenarioAct.keyName = ?1 and p.name = ?2")
    Optional<TemplateTask> findByTemplateScenarioActKeyNameAndName(String scenarioActTemplateName, String name);
}

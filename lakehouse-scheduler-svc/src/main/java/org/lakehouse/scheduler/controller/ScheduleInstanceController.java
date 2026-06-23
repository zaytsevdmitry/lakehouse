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

package org.lakehouse.scheduler.controller;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.scheduler.service.ManageStateService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ScheduleInstanceController {
    private final ManageStateService manageStateService;

    public ScheduleInstanceController(
            ScheduleTaskInstanceService scheduleInstanceService, ManageStateService manageStateService) {
        this.manageStateService = manageStateService;
    }

    @GetMapping(Endpoint.SCHEDULE)
    List<ScheduleInstanceDTO> getAll() {
        return manageStateService.findAll();
    }

    @GetMapping(Endpoint.SCHEDULE_NAME)
    List<ScheduleInstanceDTO> getAllByName(@PathVariable String name, @PathVariable int limit) {
        return manageStateService.findAllByName(name, limit);
    }

    @DeleteMapping(Endpoint.SCHEDULE_ID)
    void getAllByName(@PathVariable Long id) {
        manageStateService.delete(id);
    }


}

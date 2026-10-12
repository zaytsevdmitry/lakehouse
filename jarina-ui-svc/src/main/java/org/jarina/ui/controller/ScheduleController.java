/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.ui.controller;

import org.jarina.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.jarina.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.jarina.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.jarina.ui.dto.ScheduleRequestDTO;
import org.jarina.ui.service.ScheduleService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public List<ScheduleInstanceDTO> getSchedules(@RequestBody ScheduleRequestDTO request,
                                                  Authentication authentication) {
        return scheduleService.getSchedules(request, authentication);
    }

    @GetMapping("/headers")
    public List<ScheduleHeaderDTO> getScheduleHeaders() {
        return scheduleService.getScheduleHeaders();
    }

    @GetMapping("/dag/{id}")
    public ScheduleInstanceDAGDTO getScheduleInstanceDAG(@PathVariable Long id) {
        return scheduleService.getScheduleInstanceDAG(id);
    }
}

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

    @GetMapping(Endpoint.PROJECTS)
    List<NameSpaceDTO> findAll() {
        return nameSpaceService.getFindAll();
    }

    @PostMapping(Endpoint.PROJECTS)
    @ResponseStatus(HttpStatus.CREATED)
    NameSpaceDTO put(@RequestBody NameSpaceDTO nameSpaceDTO) {
        return nameSpaceService.save(nameSpaceDTO);
    }

    @GetMapping(Endpoint.PROJECTS_NAME)
    NameSpaceDTO get(@PathVariable String name) {
        return nameSpaceService.findByName(name);
    }

    @DeleteMapping(Endpoint.PROJECTS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        nameSpaceService.deleteById(name);
    }
}

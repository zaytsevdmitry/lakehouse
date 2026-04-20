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

    @GetMapping(Endpoint.NAME_SPACES)
    List<NameSpaceDTO> findAll() {
        return nameSpaceService.getFindAll();
    }

    @PostMapping(Endpoint.NAME_SPACES)
    @ResponseStatus(HttpStatus.CREATED)
    NameSpaceDTO put(@RequestBody NameSpaceDTO nameSpaceDTO) {
        return nameSpaceService.save(nameSpaceDTO);
    }

    @GetMapping(Endpoint.NAME_SPACES_NAME)
    NameSpaceDTO get(@PathVariable String keyName) {
        return nameSpaceService.findByName(keyName);
    }

    @DeleteMapping(Endpoint.NAME_SPACES_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String keyName) {
        nameSpaceService.deleteById(keyName);
    }
}

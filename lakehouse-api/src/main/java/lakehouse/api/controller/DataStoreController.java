package lakehouse.api.controller;

import lakehouse.api.dto.DataStoreDTO;
import lakehouse.api.service.DataStoreService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DataStoreController {
    private final DataStoreService dataStoreService;

    public DataStoreController(DataStoreService dataStoreService) {
        this.dataStoreService = dataStoreService;
    }

    @GetMapping("/datastores")
    List<DataStoreDTO> all() {
        return dataStoreService.findAll();
    }

    @PostMapping("/datastores")
    @ResponseStatus(HttpStatus.CREATED)
    DataStoreDTO put(@RequestBody DataStoreDTO dataStoreDTO) {
        return dataStoreService.save(dataStoreDTO);
    }

    @GetMapping("/datastores/{name}")
    DataStoreDTO get(@PathVariable String name) {
        return dataStoreService.findById(name);
    }

    @DeleteMapping("/datastores/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        dataStoreService.deleteById(name);
    }
}

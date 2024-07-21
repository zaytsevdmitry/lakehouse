package lakehouse.api.controller;

import lakehouse.api.dto.DataSetDTO;
import lakehouse.api.service.DataSetService;
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
public class DataSetController {
    private final DataSetService dataSetService;

    public DataSetController(DataSetService dataSetService) {
        this.dataSetService = dataSetService;
    }


    @GetMapping("/datasets")
    List<DataSetDTO> getAll() {
        return dataSetService.findAll();
    }

    @PostMapping("/datasets")
    @ResponseStatus(HttpStatus.CREATED)
    DataSetDTO post(@RequestBody DataSetDTO dataSetDTO) {
        return dataSetService.save(dataSetDTO);
    }


    @GetMapping("/datasets/{name}")
    DataSetDTO get(@PathVariable String name) {
        return dataSetService.findById(name);
    }


    @DeleteMapping("/datasets/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        dataSetService.deleteById(name);
    }
}

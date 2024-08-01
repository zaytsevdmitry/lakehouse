package lakehouse.api.configurationtest;

import lakehouse.api.dto.*;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

@Component
public class FileLoader {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public ProjectDTO loadProjectDTO() throws IOException {

        return objectMapper.readValue(new File("src/test/resources/projects-DEMO.json"), ProjectDTO.class);
    }

    public TaskExecutionServiceGroupDTO loadTaskExecutionServiceGroupDTO() throws IOException {

        return objectMapper.readValue(new File("src/test/resources/taskexecutionservicegroups-default.json"), TaskExecutionServiceGroupDTO.class);
    }

    public DataStoreDTO loadDataStoreDTO(String name) throws IOException {
        return objectMapper
                .readValue(
                        new File(String.format("src/test/resources/datastores-%s.json", name)),
                        DataStoreDTO.class);
    }

    public ScenarioDTO loadScenarioDTO() throws IOException {
        return objectMapper.readValue(new File("src/test/resources/scenarios-scenario1.json"), ScenarioDTO.class);
    }

    public <T> T stringToObject(String string, Class<T> clazz) throws IOException {
        return objectMapper.readValue(
                string,
                clazz);

    }

    public DataSetDTO loadDataSetDTO(String name) throws IOException {
        return objectMapper.readValue(new File(String.format("src/test/resources/datasets-%s.json", name)), DataSetDTO.class);

    }

    public ScheduleDTO loadScheduleDTO(String name) throws Exception {
        return objectMapper.readValue(new File(String.format("src/test/resources/schedules-%s.json", name)), ScheduleDTO.class);

    }
}

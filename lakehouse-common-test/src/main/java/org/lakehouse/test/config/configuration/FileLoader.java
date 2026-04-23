package org.lakehouse.test.config.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class FileLoader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String rootPath = "../lakehouse-config-svc/demo";
    public final String modelsDir = rootPath.concat("/sql-scripts");
    public final String dataSourcesDir = rootPath.concat("/datasources");
    public final String driversDir = rootPath.concat("/drivers");
    public final String qualityMetricsDir = rootPath.concat("/quality-metrics");

    private List<String> getFilenames(String directoryName) {
        List<String> result = new ArrayList<>();
        for (File file: Objects.requireNonNull(new File(directoryName).listFiles())){
            if (file.isFile()){
                result.add(file.getName().split("\\.")[0]);
            }else{
                List<String> dirfiles = getFilenames(directoryName + "/" +file.getName());
                dirfiles.forEach(s -> result.add(file.getName().split("\\.")[0] + "/" + s ));
            }
        }
        return result;
    }

    public NameSpaceDTO loadNameSpaceDTO() throws IOException {
        return objectMapper.readValue(new File(rootPath.concat("/name-spaces/demo.json")), NameSpaceDTO.class);
    }

    public TaskExecutionServiceGroupDTO loadTaskExecutionServiceGroupDTO() throws IOException {
        return objectMapper.readValue(new File(rootPath.concat("/taskexecutionservicegroups/default.json")),
                TaskExecutionServiceGroupDTO.class);
    }

    public DataSourceDTO loadDataSourceDTO(String name) throws IOException {
        return objectMapper.readValue(new File(String.format(dataSourcesDir.concat("/%s.json"), name)), DataSourceDTO.class);

    }
    public DriverDTO loadDriverDTO(String name) throws IOException {
        return objectMapper
                .readValue(
                        new File(String.format(driversDir.concat("/%s.json"), name)), DriverDTO.class);
    }
    public Map<String, DriverDTO> loadAllDrivers() {
        return getFilenames(driversDir)
                .stream()
                .map(name -> {
                            try {
                              return   loadDriverDTO(name);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).collect(Collectors.toMap(DriverDTO::getKeyName,Function.identity()));
    }
        public Map<String, DataSourceDTO> loadAllDataSources() {
        return getFilenames(dataSourcesDir)
                .stream()
                .map(name -> {
                    try {
                        return loadDataSourceDTO(name);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toMap(DataSourceDTO::getKeyName, Function.identity()));
    }

    public ScenarioActTemplateDTO loadScenarioDTO() throws IOException {
        return objectMapper
                .readValue(
                        new File(
                                rootPath.concat("/scenario-act-templates/default.json")), ScenarioActTemplateDTO.class);
    }

    private final String dataSetDir = rootPath.concat("/datasets");

    public DataSetDTO loadDataSetDTO(String name) throws IOException {
        logger.info("Load file of DataSetDTO {}", name);
        return objectMapper.readValue(new File(String.format(dataSetDir.concat("/%s.json"), name)), DataSetDTO.class);
    }

    public Map<String, DataSetDTO> loadAllDataSets() {
        return getFilenames(dataSetDir)
                .stream()
                .filter(string -> !string.equals("transaction_dds_v2"))
                .map(name -> {
                    try {
                        return loadDataSetDTO(name);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(
                        Collectors
                                .toMap(
                                        DataSetDTO::getKeyName,
                                        dataSetDTO -> dataSetDTO));
    }

    public ScheduleDTO loadScheduleDTO(String name) throws Exception {
        return objectMapper.readValue(new File(String.format(rootPath.concat("/schedules/%s.json"), name)), ScheduleDTO.class);
    }

    public ScenarioActTemplateDTO loadScenarioActTemplateDTO() throws IOException {
        return objectMapper.readValue(new File(rootPath.concat("/scenario-act-templates/default.json")), ScenarioActTemplateDTO.class);

    }
    public ScenarioActTemplateDTO loadScenarioActTemplateDTOcycled() throws IOException {
        return objectMapper.readValue(new File(rootPath.concat("/scenario-act-templates/cycled.json")), ScenarioActTemplateDTO.class);

    }
    public ScheduleEffectiveDTO loadScheduleEffectiveDTO() throws IOException {
        return objectMapper.readValue(new File(rootPath.concat("/schedules_effective/initial.json")), ScheduleEffectiveDTO.class);
    }


    public String loadModelScript(String name) throws IOException {
        String result = null;
        File file = new File(String.format(modelsDir.concat("/%s.sql"), name));


        DataInputStream reader = new DataInputStream(new FileInputStream(file));
        int nBytesToRead = reader.available();
        if (nBytesToRead > 0) {
            byte[] bytes = new byte[nBytesToRead];
            reader.read(bytes);
            result = new String(bytes);
        }

        return result;
    }


    public Map<String, String> loadAllModelScripts() throws IOException {
        Map<String, String> result = new HashMap<>();

        for (String name : getFilenames(modelsDir)
        ) {
            result.put(name.concat(".sql").replaceAll("/","."), loadModelScript(name));
        }
        return result;
    }

    public QualityMetricsConfDTO loadQualityMetricsConfDTO(String name) throws IOException {
        return objectMapper.readValue(new File(qualityMetricsDir.concat(String.format("/%s.json", name))), QualityMetricsConfDTO.class);
    }

    public Map<String,QualityMetricsConfDTO> loadQualityMetricsConfDTOAll() throws IOException {
        Map<String, QualityMetricsConfDTO> result = new HashMap<>();

        for (String name : getFilenames(qualityMetricsDir)
        ) {
            result.put(name.replaceAll("/","."), loadQualityMetricsConfDTO(name));
        }
        return result;
    }

}

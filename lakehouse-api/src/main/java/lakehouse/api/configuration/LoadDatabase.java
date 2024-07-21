package lakehouse.api.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lakehouse.api.entities.DagTaskEdge;
import lakehouse.api.entities.DataSet;
import lakehouse.api.entities.DataSetColumn;
import lakehouse.api.entities.DataSetProperty;
import lakehouse.api.entities.DataStore;
import lakehouse.api.entities.DataStoreProperty;
import lakehouse.api.entities.Project;
import lakehouse.api.entities.ScenarioTemplate;
import lakehouse.api.entities.Schedule;
import lakehouse.api.entities.TaskExecutionServiceGroup;
import lakehouse.api.entities.TaskTemplate;
import lakehouse.api.repository.DagTaskEdgeRepository;
import lakehouse.api.repository.DataSetColumnRepository;
import lakehouse.api.repository.DataSetPropertyRepository;
import lakehouse.api.repository.DataSetRepository;
import lakehouse.api.repository.DataStorePropertyRepository;
import lakehouse.api.repository.DataStoreRepository;
import lakehouse.api.repository.ProjectRepository;
import lakehouse.api.repository.ScenarioTemplateRepository;
import lakehouse.api.repository.ScheduleRepository;
import lakehouse.api.repository.TaskExecutionServiceGroupRepository;
import lakehouse.api.repository.TaskTemplatePropertyRepository;
import lakehouse.api.repository.TaskTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@Configuration
public class LoadDatabase {
    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static final Logger log = LoggerFactory.getLogger(LoadDatabase.class);


    @Bean
    CommandLineRunner initDemo(
            DataSetRepository dataSetRepository,
            ProjectRepository projectRepository,
            DataStoreRepository dataStoreRepository,
            DataStorePropertyRepository dataStorePropertyRepository,
            DataSetColumnRepository dataSetColumnRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            ScenarioTemplateRepository scenarioTemplateRepository,
            ScheduleRepository scheduleRepository,
            TaskTemplateRepository taskTemplateRepository,
            TaskTemplatePropertyRepository taskTemplatePropertyRepository,
            DagTaskEdgeRepository dagTaskEdgeRepository ,
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository) {

        Project project = new Project(
                "DEMO",
                "demo",
                "Demo project");

        DataStore mydb = new DataStore();
        mydb.setKey("mydb");
        mydb.setInterfaceType("jdbc");
        mydb.setVendor("postgres");
        mydb.setComment("Source database");


        List<DataStoreProperty> dataStoreProperties = new ArrayList<>();

        dataStoreProperties.add(new DataStoreProperty("url", "jdbc:mysql:mydb.com", mydb));
        dataStoreProperties.add(new DataStoreProperty("user", "lakehauseplatform", mydb));
        dataStoreProperties.add(new DataStoreProperty("password", "${very-secret-password}", mydb));

        DataStore someelsedb = new DataStore();
        someelsedb.setKey("someelsedb");
        someelsedb.setInterfaceType("filesystem");
        someelsedb.setVendor("s3");
        someelsedb.setComment("dest database");
        dataStoreProperties.add(new DataStoreProperty("url", "s3://somepath",someelsedb));
        dataStoreProperties.add(new DataStoreProperty("tokenName", "lakehauseplatform",someelsedb));
        dataStoreProperties.add(new DataStoreProperty("token", "${token}",someelsedb));

        DataSet otherTable = new DataSet();
        otherTable.setKey("otherTable");
        otherTable.setDataStore(mydb);
        otherTable.setProject(project);

        DataSet anotherTable = new DataSet();
        anotherTable.setKey("anotherTable");
        anotherTable.setDataStore(mydb);
        anotherTable.setProject(project);

        DataSet mytabledataSet = new DataSet();
        mytabledataSet.setKey("mytabledataSet");
        mytabledataSet.setDataStore(someelsedb);
        mytabledataSet.setProject(project);

        List<DataSetColumn> columns = new ArrayList<DataSetColumn>();

        DataSetColumn one = new DataSetColumn();
        DataSetColumn two = new DataSetColumn();
        DataSetColumn three = new DataSetColumn();

        one.setName("one");
        one.setComment("");
        one.setDataType("int");
        one.setNullable(false);
        one.setDataSet(mytabledataSet);

        two.setName("two");
        two.setComment("");
        two.setDataType("int");
        two.setNullable(false);
        two.setDataSet(mytabledataSet);

        three.setName("three");
        three.setComment("");
        three.setDataType("int");
        three.setNullable(false);
        three.setDataSet(mytabledataSet);

        columns.add(one);
        columns.add(two);
        columns.add(three);

        DataSetProperty otherTableDataSetProperty = new DataSetProperty();
        otherTableDataSetProperty.setDataSet(otherTable);
        otherTableDataSetProperty.setKey("data-end-point");
        otherTableDataSetProperty.setKey("mytabs.otherTable");

        DataSetProperty anotherTableDataSetProperty = new DataSetProperty();
        anotherTableDataSetProperty.setDataSet(anotherTable);
        anotherTableDataSetProperty.setKey("data-end-point");
        anotherTableDataSetProperty.setKey("mytabs.anotherTable");


        DataSetProperty myDataSetProperty = new DataSetProperty();
        myDataSetProperty.setDataSet(mytabledataSet);
        myDataSetProperty.setKey("data-end-point");
        myDataSetProperty.setKey("/mytabs/mytable");

        TaskExecutionServiceGroup defaultTaskExecutionServiceGroup = new TaskExecutionServiceGroup();
        defaultTaskExecutionServiceGroup.setKey("default");

        List<String> names = Arrays.asList("load", "merge", "dataQualityBefore","apply",
                "dataQualityAfterCritical","dataQualityAfterWarn", "finally");


       List<TaskTemplate> taskTemplates =  names.stream().map(s -> {
            String cap = s.substring(0,1).toUpperCase() + s.substring(1);

            TaskTemplate taskTemplate = new TaskTemplate();
            taskTemplate.setKey(s);
            taskTemplate.setComment("load from remote datastore");
            taskTemplate.setImportance(s.equals("dataQualityBefore")?"warn": "critical");
            taskTemplate.setExecutionModule(String.format("example.datamanipulation.%s", cap));
            taskTemplate.setTaskExecutionServiceGroup(defaultTaskExecutionServiceGroup);
            return taskTemplate;
        }).toList();

     ScenarioTemplate scenarioTemplate = new ScenarioTemplate();
   //     scenarioTemplate.setTaskTemplates(getTaskTemplates());
        //  scenarioTemplate.setInternalDataSets(internalDataSets);
        scenarioTemplate.setKey("scenario1");
        scenarioTemplate.setComment("Default scenario");


        List<DagTaskEdge> dagTaskEdges = new ArrayList<>();

        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "load".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "merge".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));
        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "merge".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "dataQualityBefore".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));
        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "dataQualityBefore".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));

        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterCritical".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));
        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterCritical".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "finally".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));
        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterWarn".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));
        dagTaskEdges.add( new DagTaskEdge(
                scenarioTemplate,
                taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterWarn".equals(taskTemplate.getKey())).findFirst().orElseThrow(),
                taskTemplates.stream().filter(taskTemplate -> "finally".equals(taskTemplate.getKey())).findFirst().orElseThrow()
        ));





        Schedule scheduleRegular = new Schedule();
        scheduleRegular.setKey("regular");
        scheduleRegular.setKey("regular schedule for mytabledataSet");
        scheduleRegular.setScenarioTemplate(scenarioTemplate);
        scheduleRegular.setDataSet(mytabledataSet);
        scheduleRegular.setIntervalExpression("0 * * * *");
        scheduleRegular.setStartDateTime(new Timestamp(new Date().getTime()));
        scheduleRegular.setScenarioTemplate(scenarioTemplate);

        Schedule scheduleInitial = new Schedule();
        scheduleInitial.setKey("initial");
        scheduleInitial.setKey("initial schedule for mytabledataSet");
        scheduleInitial.setScenarioTemplate(scenarioTemplate);
        scheduleInitial.setDataSet(mytabledataSet);
        scheduleInitial.setIntervalExpression("0 0 1 1 *");
        scheduleInitial.setStartDateTime(Timestamp.valueOf("2000-01-01 00:00:00.0000"));
        scheduleInitial.setScenarioTemplate(scenarioTemplate);

        System.out.println(asJsonString(scenarioTemplate));

        return args -> {

            log.info("Preloading " + projectRepository.save(project));
            log.info(asJsonString(project));
            projectRepository.flush();

            Arrays.asList(mydb,someelsedb).forEach( dataStore -> {
                log.info(asJsonString(dataStore));
                log.info("Preloading " + dataStoreRepository.save(dataStore));
            });
            dataStoreProperties.forEach(dataStoreProperty -> {
                log.info(asJsonString(dataStoreProperty));
                log.info("Preloading " + dataStorePropertyRepository.save(dataStoreProperty));

            });
            Arrays.asList(otherTable,anotherTable,mytabledataSet)
                    .forEach(dataSet -> {
                        log.info(asJsonString(dataSet));
                        log.info("Preloading " + dataSetRepository.save(dataSet));
                    });

            columns.forEach(dataSetColumn -> {
                log.info(asJsonString(dataSetColumn));
                log.info("Preloading " + dataSetColumnRepository.save(dataSetColumn));
            });

            Arrays.asList(otherTableDataSetProperty,anotherTableDataSetProperty,myDataSetProperty)
                    .forEach(dataSetProperty -> {
                log.info(asJsonString(dataSetProperty));
                log.info("Preloading " + dataSetPropertyRepository.save(dataSetProperty));
            });

            log.info("Preloading " + scenarioTemplateRepository.save(scenarioTemplate));
            log.info(asJsonString(scenarioTemplate));

            taskExecutionServiceGroupRepository.save(defaultTaskExecutionServiceGroup);

            taskTemplates.forEach(taskTemplate -> {
                log.info(asJsonString(taskTemplates));
                log.info("Preloading " + taskTemplateRepository.save(taskTemplate));
            });

            dagTaskEdges.forEach(dagTaskEdge -> {
                log.info(asJsonString(taskTemplates));
                log.info("Preloading " + dagTaskEdgeRepository.save(dagTaskEdge));
            });

           Arrays.asList(scheduleRegular,scheduleInitial).forEach(schedule -> {
                log.info(asJsonString(schedule));
                log.info("Preloading " + scheduleRepository.save(schedule));
            });
        };
    }

}

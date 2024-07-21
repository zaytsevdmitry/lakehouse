package lakehouse.api.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lakehouse.api.entities.DagTaskEdge;
import lakehouse.api.entities.DataSet;
import lakehouse.api.entities.DataSetColumn;
import lakehouse.api.entities.DataSetProperty;
import lakehouse.api.entities.DataSetSource;
import lakehouse.api.entities.DataSetSourceProperty;
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
import lakehouse.api.repository.DataSetSourcePropertyRepository;
import lakehouse.api.repository.DataSetSourceRepository;
import lakehouse.api.repository.DataStorePropertyRepository;
import lakehouse.api.repository.DataStoreRepository;
import lakehouse.api.repository.ExecutionModuleArgRepository;
import lakehouse.api.repository.ProjectRepository;
import lakehouse.api.repository.ScenarioTemplateRepository;
import lakehouse.api.repository.ScheduleRepository;
import lakehouse.api.repository.TaskExecutionServiceGroupRepository;
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
import java.util.Optional;


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


    private Project makeProject(ProjectRepository projectRepository) {
        Project project = new Project();
        project.setName("DEMO");
        project.setDescription("Demo project");

        return projectRepository.save(project);
    }

    private DataStore makeDataStore(DataStoreRepository dataStoreRepository, String name, String interfaceType, String vendor, String description) {
        DataStore dataStore = new DataStore();
        dataStore.setName(name);
        dataStore.setInterfaceType(interfaceType);
        dataStore.setVendor(vendor);
        dataStore.setDescription(description);
        if (dataStoreRepository.findById(dataStore.getName()).isEmpty())
            return dataStoreRepository.save(dataStore);
        else
            return dataStore;
    }

    private DataSet makeDataSet(DataSetRepository dataSetRepository, String name, DataStore dataStore, Project project, String description) {
        DataSet dataSet = new DataSet();
        dataSet.setName(name);
        dataSet.setDataStore(dataStore);
        dataSet.setProject(project);
        dataSet.setDescription(description);
        return dataSetRepository.save(dataSet);
    }
    @Bean
        //@ConditionalOnProperty(value = "load-demo")
    CommandLineRunner initDemo(
            DataSetRepository dataSetRepository,
            ProjectRepository projectRepository,
            DataStoreRepository dataStoreRepository,
            DataStorePropertyRepository dataStorePropertyRepository,
            DataSetColumnRepository dataSetColumnRepository,
            DataSetPropertyRepository dataSetPropertyRepository,
            DataSetSourceRepository dataSetSourceRepository,
            DataSetSourcePropertyRepository dataSetSourcePropertyRepository,
            ScenarioTemplateRepository scenarioTemplateRepository,
            ScheduleRepository scheduleRepository,
            TaskTemplateRepository taskTemplateRepository,
            ExecutionModuleArgRepository taskTemplatePropertyRepository,
            DagTaskEdgeRepository dagTaskEdgeRepository ,
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository) {
        return args -> {
            Project project = makeProject(projectRepository);

      /*  DataStore mydb = new DataStore();
            mydb.setName("mydb");
        mydb.setInterfaceType("jdbc");
        mydb.setVendor("postgres");
            mydb.setDescription("Source database");

*/
            DataStore mydb = makeDataStore(dataStoreRepository, "mydb", "filesystem", "s3", "Local datastore");

            DataStore someelsedb = makeDataStore(dataStoreRepository, "someelsedb", "jdbc", "postgres", "Remote datastore");

            List<DataStoreProperty> dataStoreProperties = new ArrayList<>();

            dataStoreProperties.add(new DataStoreProperty("url", "jdbc:mysql:mydb.com", someelsedb));
            dataStoreProperties.add(new DataStoreProperty("user", "lakehauseplatform", someelsedb));
            dataStoreProperties.add(new DataStoreProperty("password", "${very-secret-password}", someelsedb));

        /*DataStore someelsedb = new DataStore();
            someelsedb.setName("someelsedb");
        someelsedb.setInterfaceType("filesystem");
        someelsedb.setVendor("s3");
            someelsedb.setDescription("dest database");
          dataStoreRepository.save(datas)
          */
            dataStoreProperties.add(new DataStoreProperty("url", "s3://somepath", mydb));
            dataStoreProperties.add(new DataStoreProperty("tokenName", "lakehauseplatform", mydb));
            dataStoreProperties.add(new DataStoreProperty("token", "${token}", mydb));
            scheduleRepository.deleteById("regular");
            scheduleRepository.deleteById("initial");
            dataSetRepository.deleteById("mytabledataSet");
            dataSetRepository.deleteById("otherTable");
            dataSetRepository.deleteById("anotherTable");
            DataSet otherTable = makeDataSet(dataSetRepository, "otherTable", someelsedb, project, "remote dataset 1");
            DataSet anotherTable = makeDataSet(dataSetRepository, "anotherTable", someelsedb, project, "remote dataset 2");
            DataSet mytabledataSet = makeDataSet(dataSetRepository, "mytabledataSet", mydb, project, "Local target dataset");

  /*      DataSet otherTable = new DataSet();
        otherTable.setName("otherTable");
        otherTable.setDataStore(someelsedb);
        otherTable.setProject(project);
        otherTable = dataSetRepository.save(otherTable);

        DataSet anotherTable = new DataSet();
        anotherTable.setName("anotherTable");
        anotherTable.setDataStore(someelsedb);
        anotherTable.setProject(project);
        anotherTable = dataSetRepository.save(anotherTable);

        DataSet mytabledataSet = new DataSet();
            mytabledataSet.setName("mytabledataSet");
        mytabledataSet.setDataStore(mydb);
        mytabledataSet.setProject(project);
        mytabledataSet = dataSetRepository.save(mytabledataSet);
*/
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
            otherTableDataSetProperty.setValue("mytabs.otherTable");

        DataSetProperty anotherTableDataSetProperty = new DataSetProperty();
        anotherTableDataSetProperty.setDataSet(anotherTable);
        anotherTableDataSetProperty.setKey("data-end-point");
            anotherTableDataSetProperty.setValue("mytabs.anotherTable");


        DataSetProperty myDataSetProperty = new DataSetProperty();
        myDataSetProperty.setDataSet(mytabledataSet);
        myDataSetProperty.setKey("data-end-point");
            myDataSetProperty.setValue("/mytabs/mytable");

            DataSetSource otherTableDataSetSource = new DataSetSource();
            otherTableDataSetSource.setSource(otherTable);
            otherTableDataSetSource.setDataSet(mytabledataSet);

            DataSetSource anotherTableDataSetSource = new DataSetSource();
            anotherTableDataSetSource.setSource(anotherTable);
            anotherTableDataSetSource.setDataSet(mytabledataSet);

        TaskExecutionServiceGroup defaultTaskExecutionServiceGroup = new TaskExecutionServiceGroup();
            defaultTaskExecutionServiceGroup.setName("default");

            List<String> names = Arrays.asList("load", "merge", "dataQualityBefore", "apply",
                    "dataQualityAfterCritical", "dataQualityAfterWarn", "finally");


            ScenarioTemplate scenarioTemplate = new ScenarioTemplate();
            //     scenarioTemplate.setTaskTemplates(getTaskTemplates());
            //  scenarioTemplate.setInternalDataSets(internalDataSets);
            scenarioTemplate.setName("scenario1");
            scenarioTemplate.setDescription("Default scenario");

            List<TaskTemplate> taskTemplates = names.stream().map(s -> {
                String cap = s.substring(0, 1).toUpperCase() + s.substring(1);

            TaskTemplate taskTemplate = new TaskTemplate();
                taskTemplate.setName(s);
                taskTemplate.setScenarioTemplate(scenarioTemplate);
                taskTemplate.setDescription("load from remote datastore");
                taskTemplate.setImportance(s.equals("dataQualityBefore") ? "warn" : "critical");
            taskTemplate.setExecutionModule(String.format("example.datamanipulation.%s", cap));
            taskTemplate.setTaskExecutionServiceGroup(defaultTaskExecutionServiceGroup);
            return taskTemplate;
        }).toList();

        List<DagTaskEdge> dagTaskEdges = new ArrayList<>();

            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "load".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "merge".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));
            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "merge".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityBefore".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));
            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityBefore".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));

            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterCritical".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));
            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterCritical".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "finally".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));
            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "apply".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterWarn".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));
            dagTaskEdges.add(new DagTaskEdge(
                scenarioTemplate,
                    taskTemplates.stream().filter(taskTemplate -> "dataQualityAfterWarn".equals(taskTemplate.getName())).findFirst().orElseThrow(),
                    taskTemplates.stream().filter(taskTemplate -> "finally".equals(taskTemplate.getName())).findFirst().orElseThrow()
        ));


        Schedule scheduleRegular = new Schedule();
            scheduleRegular.setName("regular");
            scheduleRegular.setDescription("regular schedule for mytabledataSet");
        scheduleRegular.setScenarioTemplate(scenarioTemplate);
        scheduleRegular.setDataSet(mytabledataSet);
        scheduleRegular.setIntervalExpression("0 * * * *");
        scheduleRegular.setStartDateTime(new Timestamp(new Date().getTime()));
        scheduleRegular.setScenarioTemplate(scenarioTemplate);

        Schedule scheduleInitial = new Schedule();
            scheduleInitial.setName("initial");
            scheduleInitial.setDescription("initial schedule for mytabledataSet");
        scheduleInitial.setScenarioTemplate(scenarioTemplate);
        scheduleInitial.setDataSet(mytabledataSet);
        scheduleInitial.setIntervalExpression("0 0 1 1 *");
        scheduleInitial.setStartDateTime(Timestamp.valueOf("2000-01-01 00:00:00.0000"));
        scheduleInitial.setScenarioTemplate(scenarioTemplate);

        System.out.println(asJsonString(scenarioTemplate));

/*
            Arrays.asList(mydb,someelsedb).forEach( dataStore -> {
                log.info(asJsonString(dataStore));
                log.info("Preloading " + dataStoreRepository.save(dataStore));
            });
    */
            dataStorePropertyRepository.findByDataStoreName(mydb.getName()).forEach(dataStorePropertyRepository::delete);
            dataStorePropertyRepository.findByDataStoreName(someelsedb.getName()).forEach(dataStorePropertyRepository::delete);
            dataStoreProperties.forEach(dataStoreProperty -> {
                log.info(asJsonString(dataStoreProperty));
                log.info("Preloading dataStoreProperty" + dataStorePropertyRepository.save(dataStoreProperty));

            });
  /*          Arrays.asList(otherTable,anotherTable,mytabledataSet)
                    .forEach(dataSet -> {
                        log.info(asJsonString(dataSet));
                        log.info("Preloading " + dataSetRepository.save(dataSet));
                    });
*/
            columns.forEach(dataSetColumn -> {
                log.info(asJsonString(dataSetColumn));
                log.info("Preloading dataSetColumn " + dataSetColumnRepository.save(dataSetColumn));
            });

            Arrays.asList(otherTableDataSetProperty, anotherTableDataSetProperty, myDataSetProperty)
                    .forEach(dataSetProperty -> {
                        log.info(asJsonString(dataSetProperty));
                        log.info("Preloading " + dataSetPropertyRepository.save(dataSetProperty));
                    });

            Arrays.asList(otherTableDataSetSource, anotherTableDataSetSource).forEach(dataSetSource -> {

                Optional<DataSetSource> s = dataSetSourceRepository
                        .findBySourceAndDataSetName(
                                dataSetSource.getSource().getName(),
                                dataSetSource.getDataSet().getName()
                        );
                if (s.isPresent()) {
                    dataSetSourcePropertyRepository
                            .findBySourceId(s.get().getId())
                            .forEach(dataSetSourceProperty -> dataSetSourcePropertyRepository.delete(dataSetSourceProperty));
                    dataSetSourceRepository.delete(s.get());
                }
                DataSetSource src = dataSetSourceRepository.save(dataSetSource);
                DataSetSourceProperty dataSetSourceProperty = new DataSetSourceProperty();
                dataSetSourceProperty.setDataSetSource(src);
                dataSetSourceProperty.setName("optimize.query.fast:)");
                dataSetSourceProperty.setValue("true");
                dataSetSourcePropertyRepository.save(dataSetSourceProperty);

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

            Arrays.asList(scheduleRegular, scheduleInitial).forEach(schedule -> {
                log.info(asJsonString(schedule));
                log.info("Preloading " + scheduleRepository.save(schedule));
            });
        };
    }

}

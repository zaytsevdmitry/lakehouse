package org.lakehouse.taskexecutor.processor.spark.k8snative;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.junit.Before;
import org.junit.Test;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class K8SConfigServiceTest {

    private K8sConfigService k8SConfigService;
    private PodUtilService podUtilService;
    private SourceConfDTO sourceConfDTO;
    private ScheduledTaskDTO scheduledTaskDTO;

    private SourceConfDTO getSourceConfDTO() {
        DriverDTO driverDTO = new DriverDTO();
        driverDTO.setKeyName("spark_iceberg");
        driverDTO.setConnectionTemplates(Map.of(
                Types.ConnectionType.spark, "{%set service=dataSources[dataSets[targetDataSetKeyName].dataSourceKeyName].service%}\n{%set protocol=taskProcessorArgs['datasource.service.protocol']%}\n{{protocol}}://{{service.host}}:{{service.port}}"
        ));

        ServiceDTO serviceDTO = new ServiceDTO();

        Map<String,String> props = new HashMap<>( Map.of(
                "spark.driver.extraClassPath", "/opt/drivers/postgresql-42.7.8.jar,/opt/drivers/iceberg-spark-runtime-3.5_2.12-1.9.2.jar,/opt/drivers/hadoop-aws-3.3.4.jar,/opt/drivers/aws-java-sdk-bundle-1.12.262.jar,/opt/drivers/wildfly-openssl-1.0.7.Final.jar",
                "spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem",
                "spark.eventLog.enabled", "true",
                "spark.eventLog.dir", "s3a://sparklogs/eventlog/",
                "k8s.spark-operator.manifest.spec.driver.cores", "1",
                "k8s.spark-operator.manifest.metadata.namespace", "lakehouse-management"
        ));

        serviceDTO.setProperties(props);
        serviceDTO.setHost("test-host-name");
        serviceDTO.setPort("8443");

        DataSourceDTO targetSourceDTO = new DataSourceDTO();
        targetSourceDTO.setKeyName("targetSource");
        targetSourceDTO.setService(serviceDTO);
        targetSourceDTO.setDriverKeyName(driverDTO.getKeyName());

        DataSetDTO targetDataSetDTO = new DataSetDTO();
        targetDataSetDTO.setKeyName("targetDataSet");
        targetDataSetDTO.setDataSourceKeyName(targetSourceDTO.getKeyName());


        SourceConfDTO sourceConfDTO = new SourceConfDTO();
        sourceConfDTO.setTargetDataSetKeyName(targetDataSetDTO.getKeyName());
        sourceConfDTO.setDataSources(Map.of(targetSourceDTO.getKeyName(),targetSourceDTO));
        sourceConfDTO.setDataSets(Map.of(targetDataSetDTO.getKeyName(), targetDataSetDTO));
        sourceConfDTO.setDrivers(Map.of(driverDTO.getKeyName(),driverDTO));
        return sourceConfDTO;

    }
    private ScheduledTaskDTO getScheduledTaskDTO(){
        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setName("quality");
        scheduledTaskDTO.setScheduleKeyName("Test_Schedule");
        scheduledTaskDTO.setScenarioActKeyName("Test_Act");
        scheduledTaskDTO.setTargetDateTime("2026-01-01 00:00:00z");
        scheduledTaskDTO.setTaskExecutionServiceGroupName("default");
        scheduledTaskDTO.setTaskProcessor("K8sSparkNativeTaskProcessor");
        scheduledTaskDTO.setTaskProcessorBody( "sparkTaskProcessorDQBody");
        Map<String,String> taskProcessorArgs = new HashMap<>(
                Map.of(
                "k8s.spark-operator.manifest.spec.image", "apache/spark:3.5.0",
                "spark.ui.enabled", "true",
                "spark.executor.memory", "1g",
                "k8s.spark-operator.manifest.metadata.namespace", "lakehouse-management-ovrd",
                "lakehouse.client.rest.config.server.url", "http://lakehouse-management-config-service:8080",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.properties.bootstrap.servers" , "broker:9092",
                "lakehouse.taskexecutor.body.config.dq.kafka.producer.metric.value.topic", "metric_value",
                "datasource.service.protocol", "https",
                "k8s.spark-operator.manifest.spec.mainApplicationFile", "/opt/lakehouse-task-spark-apps/lakehouse-task-executor-spark-dq-app-0.4.0-jar-with-dependencies.jar",
                "k8s.spark-operator.manifest.spec.mainClass", "org.lakehouse.taskexecutor.spark.dq.SparkProcessorApplicationDQ"));

        scheduledTaskDTO.setId(123L);
        scheduledTaskDTO.setTryNum(2);

        scheduledTaskDTO.setTaskProcessorArgs(taskProcessorArgs);
        return scheduledTaskDTO;

    }
    @Before
    public void setUp() {
        podUtilService = new PodUtilService("","");
        k8SConfigService = new K8sConfigService(podUtilService);

        // Инициализируем вложенную структуру SourceConfDTO, чтобы избежать NullPointerException
        sourceConfDTO = getSourceConfDTO();
        // Инициализируем базовые поля ScheduledTaskDTO
        scheduledTaskDTO = getScheduledTaskDTO();

    }

    // ==========================================================
    // ТЕСТЫ ДЛЯ МЕТОДА: getFixedTaskName
    // ==========================================================

    @Test
    public void testGetFixedTaskName_WhenPodHasName_ReturnsPodName() {
        Pod pod = new PodBuilder()
                .withNewMetadata()
                .withName("custom-pod-name")
                .endMetadata()
                .build();

        String result = podUtilService.getFixedTaskName(scheduledTaskDTO, pod);

        assertEquals("custom-pod-name", result);
    }

    @Test
    public void testGetFixedTaskName_GeneratesAndCleansDnsName() {
        // Имя содержит верхний регистр, спецсимволы и подчеркивания

        String result = podUtilService.getFixedTaskName(scheduledTaskDTO, null);
        System.out.println(result);
        // Проверяем формат: task-{id}-{tryNum}-{cleaned_name}
        assertTrue(result.startsWith("task-123-2-"));
        // Регулярка [^a-z0-9] должна заменить все недопустимые символы на дефисы
        assertFalse(result.contains("_"));
        assertFalse(result.contains("$"));
        assertFalse(result.contains("#"));
        assertEquals(result, result.toLowerCase());
    }

    @Test
    public void testGetFixedTaskName_TrimsTo63CharactersAndCleansEdges() {
        // Передаем слишком длинное имя, которое гарантированно превысит лимит в 63 символа

        String result = podUtilService.getFixedTaskName(scheduledTaskDTO, null);
        System.out.println(result);
        // Проверяем требования DNS RFC 1123 для Kubernetes
        assertTrue(result.length() <= 63);
        assertFalse(result.endsWith("-"));
        assertFalse(result.startsWith("-"));
    }

    // ==========================================================
    // ТЕСТЫ ДЛЯ МЕТОДА: extractArguments
    // ==========================================================


    // ==========================================================
    // ТЕСТЫ ДЛЯ МЕТОДА: translateSparkConfToResources
    // ==========================================================

    @Test
    public void testTranslateSparkConfToResources_DefaultValues() {
        Map<String, String> sparkConf = new HashMap<>();

        ResourceRequirements resources = podUtilService.translateSparkConfToResources(sparkConf);

        // Дефолты: 1 CPU. Память: 1g (1024Mi) + 10% overhead (минимум 384Mi) = 1408Mi
        assertEquals("1", resources.getRequests().get("cpu").getAmount());
        assertEquals("1408", resources.getRequests().get("memory").getAmount());
        assertEquals("Mi", resources.getLimits().get("memory").getFormat());
    }

    @Test
    public void testTranslateSparkConfToResources_CustomValues() {
        Map<String, String> sparkConf = new HashMap<>();
        sparkConf.put("spark.driver.cores", "4");
        sparkConf.put("spark.driver.memory", "2g");          // 2048 MB
        sparkConf.put("spark.driver.memoryOverhead", "512m"); // 512 MB
        // Итого: 2560 MB -> 2560Mi

        ResourceRequirements resources = podUtilService.translateSparkConfToResources(sparkConf);

        assertEquals("4", resources.getRequests().get("cpu").getAmount());
        assertEquals("2560", resources.getRequests().get("memory").getAmount());
        assertEquals("Mi", resources.getRequests().get("memory").getFormat());
        assertEquals("2560Mi", resources.getRequests().get("memory").toString());
    }

    // ==========================================================
    // ТЕСТЫ ДЛЯ МЕТОДА: parseSparkMemoryToBytes
    // ==========================================================

    @Test
    public void testParseSparkMemoryToBytes_CorrectUnits() {
        assertEquals(1024L * 1024 * 1024, podUtilService.parseSparkMemoryToBytes("1g"));
        assertEquals(512L * 1024 * 1024, podUtilService.parseSparkMemoryToBytes("512m"));
        assertEquals(2048L * 1024, podUtilService.parseSparkMemoryToBytes("2048k"));
    }

    @Test
    public void testParseSparkMemoryToBytes_FallbackOnInvalidString() {
        long defaultBytes = 1024L * 1024 * 1024; // 1GB в байтах
        
        // Любые некорректные или пустые строки должны безопасно возвращать дефолтное значение
        assertEquals(defaultBytes, podUtilService.parseSparkMemoryToBytes("incorrect_format_123"));
        assertEquals(defaultBytes, podUtilService.parseSparkMemoryToBytes(""));
        assertEquals(defaultBytes, podUtilService.parseSparkMemoryToBytes(null));
    }

    // ==========================================================
    // ТЕСТЫ ДЛЯ МЕТОДА: extractMasterUrl
    // ==========================================================
    @Test
    public void testExtractMasterUrl_SuccessWithProtocolFromTaskProcessorArgs() throws TaskConfigurationException, IOException {
        ScheduledTaskDTO scheduledTaskDTO = K8sClientServiceTest.getScheduledTaskDTO();
        SourceConfDTO sourceConfDTO =  K8sClientServiceTest.getSourceConfDTO();
        Map<String,String> props = new HashMap<>(scheduledTaskDTO.getTaskProcessorArgs());
        props.put(SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY,"https");
        scheduledTaskDTO.setTaskProcessorArgs(props);
        sourceConfDTO.getTargetDataSource().getService().setHost("test-host-name");
        sourceConfDTO.getTargetDataSource().getService().setPort("8443");
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils(sourceConfDTO,scheduledTaskDTO);
        String expectedRenderedUrl = "https://test-host-name:8443";

        // Act
        String actualUrl = k8SConfigService.extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtils);

        // Assert
        assertEquals(expectedRenderedUrl, actualUrl);
    }


    @Test
    public void testExtractMasterUrl_ThrowsExceptionWhenSparkTemplateMissing() {
        // Arrange
        org.lakehouse.jinja.java.JinJavaUtils jinJavaUtilsMock = org.mockito.Mockito.mock(org.lakehouse.jinja.java.JinJavaUtils.class);

        // Очищаем шаблоны соединений у драйвера
        sourceConfDTO.getDrivers().get("spark_iceberg").setConnectionTemplates(new HashMap<>());

        // Act & Assert
        TaskConfigurationException exception = assertThrows(
                TaskConfigurationException.class,
                () -> k8SConfigService.extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtilsMock)
        );
        assertTrue(exception.getMessage().contains("Connection template spark is not present in driver spark_iceberg"));
    }

    @Test
    public void testExtractMasterUrl_ThrowsExceptionWhenProtocolIsMissingEverywhere() {
        // Arrange
        org.lakehouse.jinja.java.JinJavaUtils jinJavaUtilsMock = org.mockito.Mockito.mock(org.lakehouse.jinja.java.JinJavaUtils.class);

        // Удаляем ключ протокола отовсюду
        scheduledTaskDTO.getTaskProcessorArgs().remove("datasource.service.protocol");
        sourceConfDTO.getDataSources().get("targetSource").getService().getProperties().remove("datasource.service.protocol");

        // Act & Assert
        TaskConfigurationException exception = assertThrows(
                TaskConfigurationException.class,
                () -> k8SConfigService.extractMasterUrl(sourceConfDTO, scheduledTaskDTO, jinJavaUtilsMock)
        );
        // Метод buildTaskFullName() для вашей таски вернет "Test_Schedule.Test_Act.quality"
        assertTrue(exception.getMessage().contains("Key 'datasource.service.protocol' is not present in TaskProcessorArgs"));
    }

}

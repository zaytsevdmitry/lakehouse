package org.lakehouse.taskexecutor.test.stub;

import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.spark.SparkRestClientApi;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.client.rest.spark.standalone.StatusResponse;

import java.io.IOException;

public class SparkRestClientApiTest implements SparkRestClientApi {
    /*CreateRequest request = ObjectMapping.stringToObject("{\n" +
            "  \"appResource\": \"\",\n" +
            "  \"sparkProperties\": {\n" +
            "    \"spark.master\": \"spark://master:7077\",\n" +
            "    \"spark.app.name\": \"Spark Pi\",\n" +
            "    \"spark.driver.memory\": \"1g\",\n" +
            "    \"spark.driver.cores\": \"1\",\n" +
            "    \"spark.jars\": \"\"\n" +
            "  },\n" +
            "  \"clientSparkVersion\": \"\",\n" +
            "  \"mainClass\": \"org.apache.spark.deploy.SparkSubmit\",\n" +
            "  \"environmentVariables\": { },\n" +
            "  \"action\": \"CreateSubmissionRequest\",\n" +
            "  \"appArgs\": [ \"/opt/spark/examples/src/main/python/pi.py\", \"10\" ]\n" +
            "}", CreateRequest.class);*/


    @Override
    public CreateResponse createSubmission(CreateRequest createRequest) {
        try {
            return ObjectMapping.stringToObject("{\n" +
                    "  \"action\" : \"CreateSubmissionResponse\",\n" +
                    "  \"message\" : \"Driver successfully submitted as driver-20231124153531-0000\",\n" +
                    "  \"serverSparkVersion\" : \"3.5.1\",\n" +
                    "  \"submissionId\" : \"driver-20231124153531-0000\",\n" +
                    "  \"success\" : true\n" +
                    "}", CreateResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public StatusResponse getStatus(String submissionId) {
        return null;
    }
}

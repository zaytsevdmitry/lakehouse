package org.lakehouse.client.rest.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.spark.standalone.CreateRequest;
import org.lakehouse.client.rest.spark.standalone.CreateResponse;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@RestClientTest(properties = {
        "lakehouse.client.rest.spark.server.url=",
})
public class SparkRestClientHelperTest {

    @Autowired
    SparkRestClientApi restClientHelper;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;


    private final FileLoader fileLoader = new FileLoader();

    @Test
    public void MakesCorrectCallNameSpaceDTO() throws Exception {
        CreateRequest request = ObjectMapping.stringToObject("{\n" +
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
                "}", CreateRequest.class);

        CreateResponse responseExpect = ObjectMapping.stringToObject("{\n" +
                "  \"action\" : \"CreateSubmissionResponse\",\n" +
                "  \"message\" : \"Driver successfully submitted as driver-20231124153531-0000\",\n" +
                "  \"serverSparkVersion\" : \"3.5.1\",\n" +
                "  \"submissionId\" : \"driver-20231124153531-0000\",\n" +
                "  \"success\" : true\n" +
                "}", CreateResponse.class);


        server.expect(ExpectedCount.manyTimes(),
                        requestTo("create"))
                .andRespond(withSuccess(
                        objectMapper
                                .writeValueAsString(responseExpect),
                        MediaType.APPLICATION_JSON));


        CreateResponse response = restClientHelper.createSubmission(request);
        assert (responseExpect.equals(response));
    }

}

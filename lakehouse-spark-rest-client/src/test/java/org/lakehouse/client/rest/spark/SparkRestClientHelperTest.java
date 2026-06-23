/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

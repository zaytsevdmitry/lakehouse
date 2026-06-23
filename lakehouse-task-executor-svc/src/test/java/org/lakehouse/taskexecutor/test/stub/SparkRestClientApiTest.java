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

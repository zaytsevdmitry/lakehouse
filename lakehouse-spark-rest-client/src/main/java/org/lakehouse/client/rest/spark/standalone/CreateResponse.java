/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.client.rest.spark.standalone;

import java.util.Objects;

/*
* {
  "action" : "CreateSubmissionResponse",
  "message" : "Driver successfully submitted as driver-20231124153531-0000",
  "serverSparkVersion" : "3.5.1",
  "submissionId" : "driver-20231124153531-0000",
  "success" : true
}*/
public class CreateResponse {
    private String action;
    private String message;
    private String serverSparkVersion;
    private String submissionId;
    private String success;

    public CreateResponse() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getServerSparkVersion() {
        return serverSparkVersion;
    }

    public void setServerSparkVersion(String serverSparkVersion) {
        this.serverSparkVersion = serverSparkVersion;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreateResponse response = (CreateResponse) o;
        return Objects.equals(getAction(), response.getAction()) && Objects.equals(getMessage(), response.getMessage()) && Objects.equals(getServerSparkVersion(), response.getServerSparkVersion()) && Objects.equals(getSubmissionId(), response.getSubmissionId()) && Objects.equals(getSuccess(), response.getSuccess());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAction(), getMessage(), getServerSparkVersion(), getSubmissionId(), getSuccess());
    }
}

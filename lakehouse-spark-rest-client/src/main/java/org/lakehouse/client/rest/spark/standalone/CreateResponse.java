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

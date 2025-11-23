package org.lakehouse.client.rest.spark.standalone;

public class StatusResponse {
    /*{
  "action" : "SubmissionStatusResponse",
  "driverState" : "RUNNING",
  "serverSparkVersion" : "2.2.0",
  "submissionId" : "driver-20170829203736-0004",
  "success" : true,
  "workerHostPort" : "10.32.1.18:38317",
  "workerId" : "worker-20170829013941-10.32.1.18-38317"
}*/
    private String action;
    private String driverState;
    private String serverSparkVersion;
    private String submissionId;
    private String workerHostPort;
    private String workerId;

    public StatusResponse() {
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDriverState() {
        return driverState;
    }

    public void setDriverState(String driverState) {
        this.driverState = driverState;
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

    public String getWorkerHostPort() {
        return workerHostPort;
    }

    public void setWorkerHostPort(String workerHostPort) {
        this.workerHostPort = workerHostPort;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }
}

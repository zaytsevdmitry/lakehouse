package org.lakehouse.client.api.dto.scheduler.lock;

import org.lakehouse.client.api.constant.Status;

public class TaskResultDTO {
    Status.Task status;
    String causes;

    public TaskResultDTO() {

    }

    public TaskResultDTO(Status.Task status, String causes) {
        this.status = status;
        this.causes = causes;
    }

    public TaskResultDTO(Status.Task status) {
        this.status = status;
    }

    public Status.Task getStatus() {
        return status;
    }

    public void setStatus(Status.Task status) {
        this.status = status;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }
}

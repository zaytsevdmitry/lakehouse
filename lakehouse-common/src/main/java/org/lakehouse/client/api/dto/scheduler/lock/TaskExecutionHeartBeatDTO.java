package org.lakehouse.client.api.dto.scheduler.lock;

public class TaskExecutionHeartBeatDTO {

    private Long lockId;

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

    @Override
    public String toString() {
        return "TaskExecutionHeartBeatDTO{" +
                "lockId=" + lockId +
                '}';
    }
}

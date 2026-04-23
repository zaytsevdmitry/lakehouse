package org.lakehouse.client.api.dto.scheduler.lock;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;

public class ScheduledTaskLockDTO {
    private Long lockId;
    private ScheduledTaskDTO scheduledTaskEffectiveDTO;
    private String lastHeartBeatDateTime;
    private String serviceId;


    public ScheduledTaskLockDTO() {

    }

    public Long getLockId() {
        return lockId;
    }

    public void setLockId(Long lockId) {
        this.lockId = lockId;
    }

    public ScheduledTaskDTO getScheduledTaskEffectiveDTO() {
        return scheduledTaskEffectiveDTO;
    }

    public void setScheduledTaskEffectiveDTO(ScheduledTaskDTO scheduledTaskEffectiveDTO) {
        this.scheduledTaskEffectiveDTO = scheduledTaskEffectiveDTO;
    }

    public String getLastHeartBeatDateTime() {
        return lastHeartBeatDateTime;
    }

    public void setLastHeartBeatDateTime(String lastHeartBeatDateTime) {
        this.lastHeartBeatDateTime = lastHeartBeatDateTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }



}

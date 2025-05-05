package org.lakehouse.scheduler.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class ScheduledTaskForProducerMessage{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false, foreignKey = @ForeignKey(name = "scheduled_task_for_producer_message__sti_id_fk"))
    private ScheduleTaskInstance scheduleTaskInstance;

    public ScheduledTaskForProducerMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ScheduleTaskInstance getScheduleTaskInstance() {
        return scheduleTaskInstance;
    }

    public void setScheduleTaskInstance(ScheduleTaskInstance scheduleTaskInstance) {
        this.scheduleTaskInstance = scheduleTaskInstance;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledTaskForProducerMessage message = (ScheduledTaskForProducerMessage) o;
        return Objects.equals(getId(), message.getId()) && Objects.equals(getScheduleTaskInstance(), message.getScheduleTaskInstance());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScheduleTaskInstance());
    }
}

package org.lakehouse.config.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;
@Entity
public class ScheduleProduceMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "schedule_producer_message__schedule_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Schedule schedule;

    public ScheduleProduceMessage() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleProduceMessage that = (ScheduleProduceMessage) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getSchedule(), that.getSchedule());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getSchedule());
    }

}

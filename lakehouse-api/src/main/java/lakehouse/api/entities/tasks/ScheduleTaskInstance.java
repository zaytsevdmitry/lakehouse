package lakehouse.api.entities.tasks;

import jakarta.persistence.*;
import lakehouse.api.constant.Status;
import lakehouse.api.entities.configs.TaskExecutionServiceGroup;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(uniqueConstraints =
    @UniqueConstraint(
            name = "schedule_task_instance_schedule_scenario_act_instance_id_name_uk" ,
            columnNames = {"schedule_scenario_act_instance_id","name"}))
public class ScheduleTaskInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleScenarioActInstance scheduleScenarioActInstance;

    @ManyToOne
    @JoinColumn(nullable = true)
    private TaskExecutionServiceGroup taskExecutionServiceGroup;

    @Column(nullable = true)
    private String executionModule;

    private OffsetDateTime beginDateTime;

    private OffsetDateTime endDateTime;

    @Column(nullable = false, columnDefinition = "varchar default 'NEW' CHECK (status IN ('NEW', 'SUCCESS', 'QUEUED', 'RUNNING'))")
    private String status = Status.Task.NEW.label;

    @Column(nullable = false, columnDefinition = "int default 0")
    private int reTryCount = 0;



    public ScheduleTaskInstance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
        return taskExecutionServiceGroup;
    }

    public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
        this.taskExecutionServiceGroup = taskExecutionServiceGroup;
    }

    public String getExecutionModule() {
        return executionModule;
    }

    public void setExecutionModule(String executionModule) {
        this.executionModule = executionModule;
    }

    public OffsetDateTime getBeginDateTime() {
        return beginDateTime;
    }

    public void setBeginDateTime(OffsetDateTime beginDateTime) {
        this.beginDateTime = beginDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getReTryCount() {
        return reTryCount;
    }

    public void setReTryCount(int reTryCount) {
        this.reTryCount = reTryCount;
    }



    public ScheduleScenarioActInstance getScheduleScenarioActInstance() {
        return scheduleScenarioActInstance;
    }

    public void setScheduleScenarioActInstance(
            ScheduleScenarioActInstance scheduleScenarioActInstance) {
        this.scheduleScenarioActInstance = scheduleScenarioActInstance;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleTaskInstance that = (ScheduleTaskInstance) o;
        return getReTryCount() == that.getReTryCount()
                && Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(getScheduleScenarioActInstance(), that.getScheduleScenarioActInstance()) && Objects.equals(getTaskExecutionServiceGroup(), that.getTaskExecutionServiceGroup()) && Objects.equals(getExecutionModule(), that.getExecutionModule()) && Objects.equals(getBeginDateTime(), that.getBeginDateTime()) && Objects.equals(getEndDateTime(), that.getEndDateTime()) && Objects.equals(getStatus(), that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getScheduleScenarioActInstance(), getTaskExecutionServiceGroup(), getExecutionModule(), getBeginDateTime(), getEndDateTime(), getStatus(), getReTryCount());
    }
}

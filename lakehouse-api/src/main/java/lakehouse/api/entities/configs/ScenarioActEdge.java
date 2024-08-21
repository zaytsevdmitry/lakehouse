package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "scenario_act_edge_uk"  ,columnNames = {"schedule_name","from_scenario_act_id","to_scenario_act_id"}))
public class ScenarioActEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Schedule schedule;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioAct fromScenarioAct;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScenarioAct toScenarioAct;

    public ScenarioActEdge() {
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public ScenarioAct getFromScheduleScenarioAct() {
        return fromScenarioAct;
    }

    public void setFromScheduleScenarioAct(ScenarioAct fromScenarioAct) {
        this.fromScenarioAct = fromScenarioAct;
    }

    public ScenarioAct getToScheduleScenarioAct() {
        return toScenarioAct;
    }

    public void setToScheduleScenarioAct(ScenarioAct toScenarioAct) {
        this.toScenarioAct = toScenarioAct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioActEdge that = (ScenarioActEdge) o;
        return Objects.equals(getSchedule(), that.getSchedule()) && Objects.equals(getFromScheduleScenarioAct(), that.getFromScheduleScenarioAct()) && Objects.equals(getToScheduleScenarioAct(), that.getToScheduleScenarioAct());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSchedule(), getFromScheduleScenarioAct(), getToScheduleScenarioAct());
    }
}

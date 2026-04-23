package org.lakehouse.config.entities.templates;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "task_template_edge_from_to_uk", columnNames = {"scenario_act_template_name",
                "from_task_template_id", "to_task_template_id"})})
public class TemplateTaskEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__scenario_act_template_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TemplateScenarioAct templateScenarioAct;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__from_task_template_fk"))
    private TemplateTask fromTemplateTask;

    @ManyToOne
    @JoinColumn(foreignKey = @ForeignKey(name = "task_template_edge__to_task_template_fk"))
    private TemplateTask toTemplateTask;

    public TemplateTaskEdge() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TemplateScenarioAct getScenarioActTemplate() {
        return templateScenarioAct;
    }

    public void setScenarioActTemplate(TemplateScenarioAct templateScenarioAct) {
        this.templateScenarioAct = templateScenarioAct;
    }

    public TemplateTask getFromTaskTemplate() {
        return fromTemplateTask;
    }

    public void setFromTaskTemplate(TemplateTask fromTemplateTask) {
        this.fromTemplateTask = fromTemplateTask;
    }

    public TemplateTask getToTaskTemplate() {
        return toTemplateTask;
    }

    public void setToTaskTemplate(TemplateTask toTemplateTask) {
        this.toTemplateTask = toTemplateTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TemplateTaskEdge that = (TemplateTaskEdge) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate())
                && Objects.equals(getFromTaskTemplate(), that.getFromTaskTemplate())
                && Objects.equals(getToTaskTemplate(), that.getToTaskTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getScenarioActTemplate(), getFromTaskTemplate(), getToTaskTemplate());
    }
}

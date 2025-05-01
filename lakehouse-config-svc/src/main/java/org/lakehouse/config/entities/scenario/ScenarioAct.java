package org.lakehouse.config.entities.scenario;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.DataSet;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.templates.ScenarioActTemplate;

import java.util.Objects;
/**
 * Описывает один из графов операций над датасетом.
 * Связывает датасет и расписание
 * */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "scenario_act_data_set_name_name_schedule_name_uk", columnNames = {
		"schedule_name", "name" }))
public class ScenarioAct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act_data_set_fk"))
	private DataSet dataSet;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act__schedule_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Schedule schedule;

	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "scenario_act__scenario_act_template_fk"))
	private ScenarioActTemplate scenarioActTemplate;

	public ScenarioAct() {
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

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public ScenarioActTemplate getScenarioActTemplate() {
		return scenarioActTemplate;
	}

	public void setScenarioActTemplate(ScenarioActTemplate scenarioActTemplate) {
		this.scenarioActTemplate = scenarioActTemplate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ScenarioAct that = (ScenarioAct) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName())
				&& Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getSchedule(), that.getSchedule())
				&& Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getName(), getDataSet(), getSchedule(), getScenarioActTemplate());
	}
}

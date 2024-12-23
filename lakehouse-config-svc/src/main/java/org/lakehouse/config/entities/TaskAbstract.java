package org.lakehouse.config.entities;

import jakarta.persistence.*;

import java.util.Objects;

@MappedSuperclass
public abstract class TaskAbstract  {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String name;

	private String description;


	@Column(nullable = false)
	private String executionModule;

	@Column(nullable = false)
	private String importance;


	@ManyToOne
	@JoinColumn(foreignKey = @ForeignKey(name = "task__task_execution_service_group_fk"))
	private TaskExecutionServiceGroup taskExecutionServiceGroup;

	
	public TaskAbstract() {
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return getName();
	}


	public String getExecutionModule() {
		return executionModule;
	}

	public void setExecutionModule(String executionModule) {
		this.executionModule = executionModule;
	}

	public String getImportance() {
		return importance;
	}

	public void setImportance(String importance) {
		this.importance = importance;
	}

	public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
		return taskExecutionServiceGroup;
	}

	public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
		this.taskExecutionServiceGroup = taskExecutionServiceGroup;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		TaskAbstract that = (TaskAbstract) o;
		return Objects.equals(getName(), that.getName())
				&& Objects.equals(getId(), that.getId())
				&& Objects.equals(getDescription(), that.getDescription())
				&& Objects.equals(getExecutionModule(), that.getExecutionModule())
				&& Objects.equals(getImportance(), that.getImportance())
				&& Objects.equals(getTaskExecutionServiceGroup(), that.getTaskExecutionServiceGroup());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getId(),getName(), getDescription(),
				getExecutionModule(), getImportance(), 
				getTaskExecutionServiceGroup());
	}
}

package org.lakehouse.config.entities;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class DataSet extends KeyEntityAbstract {

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set__project_fk"))
	private Project project;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set__data_store_fk"))
	private DataStore dataStore;

	@Column(nullable = false)
	private String fullTableName;

	public DataSet() {
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public String getFullTableName() {
		return fullTableName;
	}

	public void setFullTableName(String fullTableName) {
		this.fullTableName = fullTableName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		DataSet dataSet = (DataSet) o;
		return Objects.equals(getProject(), dataSet.getProject())
				&& Objects.equals(getDataStore(), dataSet.getDataStore())
				&& Objects.equals(getFullTableName(),dataSet.getFullTableName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getProject(), getDataStore(), getFullTableName());
	}
}

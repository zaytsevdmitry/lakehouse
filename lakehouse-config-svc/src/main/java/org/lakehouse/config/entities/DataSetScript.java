package org.lakehouse.config.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_script_data_set_name_source_name_uk", columnNames = {
		"data_set_name", "script_key" }))
public class DataSetScript {

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_script__script_fk"))
	Script script;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_script__data_set_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataSet dataSet;

	private Integer scriptOrder;

	public DataSetScript() {
	}

	public Script getScript() {
		return script;
	}

	public void setScript(Script script) {
		this.script = script;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	public Integer getScriptOrder() {
		return scriptOrder;
	}

	public void setScriptOrder(Integer scriptOrder) {
		this.scriptOrder = scriptOrder;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		DataSetScript that = (DataSetScript) o;
		return Objects.equals(getScript(), that.getScript()) && Objects.equals(getId(), that.getId()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getScriptOrder(), that.getScriptOrder());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getScript(), getId(), getDataSet(), getScriptOrder());
	}
}

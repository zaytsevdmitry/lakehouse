package org.lakehouse.config.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_data_set_name_source_name_uk", columnNames = {
		"data_set_name", "source_name" }))
public class DataSetSource {

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_source__data_set_cur_fk"))
	DataSet source;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_source__data_set_src_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataSet dataSet;

	public DataSetSource() {
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	public DataSet getSource() {
		return source;
	}

	public void setSource(DataSet source) {
		this.source = source;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataSetSource that = (DataSetSource) o;
		return Objects.equals(getSource(), that.getSource()) && Objects.equals(getId(), that.getId())
				&& Objects.equals(getDataSet(), that.getDataSet());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSource(), getId(), getDataSet());
	}
}

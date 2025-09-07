package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_property_data_set_source_id_key_uk", columnNames = {
		"data_set_source_id", "key" }))
public class DataSetSourceProperty extends KeyValueAbstract {


	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_source_property__data_set_source_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataSetSource dataSetSource;


	public DataSetSourceProperty() {

	}

	public DataSetSource getDataSetSource() {
		return dataSetSource;
	}

	public void setDataSetSource(DataSetSource dataSetSource) {
		this.dataSetSource = dataSetSource;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataSetSourceProperty that = (DataSetSourceProperty) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getDataSetSource(), that.getDataSetSource())
				&& Objects.equals(getKey(), that.getKey()) && Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getDataSetSource(), getKey(), getValue());
	}
}

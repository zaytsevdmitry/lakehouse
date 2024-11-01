package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_source_property_data_set_source_id_name_uk", columnNames = {
		"data_set_source_id", "name" }))
public class DataSetSourceProperty {
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_source_property__data_set_source_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataSetSource dataSetSource;

	private String name;

	private String value;

	public DataSetSourceProperty() {

	}

	public DataSetSource getDataSetSource() {
		return dataSetSource;
	}

	public void setDataSetSource(DataSetSource dataSetSource) {
		this.dataSetSource = dataSetSource;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataSetSourceProperty that = (DataSetSourceProperty) o;
		return Objects.equals(getId(), that.getId()) && Objects.equals(getDataSetSource(), that.getDataSetSource())
				&& Objects.equals(getName(), that.getName()) && Objects.equals(getValue(), that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getDataSetSource(), getName(), getValue());
	}
}

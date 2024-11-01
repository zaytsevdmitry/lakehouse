package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_column_dataset_name_name_uk", columnNames = {
		"data_set_name", "name" }))
public class DataSetColumn {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	private String dataType;
	private boolean nullable;
	private String comment;

	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_set_column__data_set_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataSet dataSet;

	public DataSetColumn() {
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

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public DataSet getDataSet() {
		return dataSet;
	}

	public void setDataSet(DataSet dataSet) {
		this.dataSet = dataSet;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DataSetColumn that = (DataSetColumn) o;
		return isNullable() == that.isNullable() && Objects.equals(getId(), that.getId())
				&& Objects.equals(getName(), that.getName()) && Objects.equals(getDataType(), that.getDataType())
				&& Objects.equals(getComment(), that.getComment()) && Objects.equals(getDataSet(), that.getDataSet());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getId(), getName(), getDataType(), isNullable(), getComment(), getDataSet());
	}
}

package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_store_property_data_store_name_key_uk", columnNames = {
		"data_store_name", "key" }))
public class DataStoreProperty extends KeyValueAbstract {
	@ManyToOne(optional = false)
	@JoinColumn(foreignKey = @ForeignKey(name = "data_store_property__data_store_fk"))
	@OnDelete(action = OnDeleteAction.CASCADE)
	private DataStore dataStore;

	public DataStoreProperty() {
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		DataStoreProperty that = (DataStoreProperty) o;
		return Objects.equals(getDataStore(), that.getDataStore());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getDataStore());
	}
}

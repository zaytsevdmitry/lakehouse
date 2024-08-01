package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"data_store_name", "key"}))
public class DataStoreProperty extends KeyValueAbstract{
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataStore dataStore;

    public DataStoreProperty() {
    }

    public DataStoreProperty(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public DataStoreProperty(String key, String value, DataStore dataStore) {
        super(key, value);
        this.dataStore = dataStore;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataStoreProperty that = (DataStoreProperty) o;
        return Objects.equals(getDataStore(), that.getDataStore());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataStore());
    }
}

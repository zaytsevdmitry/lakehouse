package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"data_store_name", "key"}))
public class DataStoreProperty extends KeyValueAbstract{
    @ManyToOne
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
}

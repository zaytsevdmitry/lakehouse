package lakehouse.api.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class DataStoreProperty extends KeyValueAbstract{
    @ManyToOne (cascade = CascadeType.REMOVE)
    @JoinColumn(name = "data_store_key", referencedColumnName = "key")
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

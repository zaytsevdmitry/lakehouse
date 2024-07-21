package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class DataSetProperty extends KeyValueAbstract{
    @ManyToOne
    @JoinColumn(name = "data_set_key", referencedColumnName = "key")
    private DataSet dataSet;

    public DataSetProperty() {}


    public DataSetProperty(String key, String value, DataSet dataSet) {
        super(key, value);
        this.dataSet = dataSet;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }
}

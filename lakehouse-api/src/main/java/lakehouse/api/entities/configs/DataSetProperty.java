package lakehouse.api.entities.configs;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "data_set_property_data_set_name_key_uk" ,
        columnNames = {"data_set_name", "key"}))
public class DataSetProperty extends KeyValueAbstract{

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_property__data_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSet dataSet;

    public DataSetProperty() {}

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataSetProperty that = (DataSetProperty) o;
        return Objects.equals(getDataSet(), that.getDataSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSet());
    }
}

package lakehouse.api.entities.configs;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.Objects;

@Entity
public class DataSet extends KeyEntityAbstract {

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__project_fk"))
    private Project project;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set__data_store_fk"))
    private DataStore dataStore;

    public DataSet() {}

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
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
        DataSet dataSet = (DataSet) o;
        return Objects.equals(getProject(), dataSet.getProject()) && Objects.equals(getDataStore(), dataSet.getDataStore());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProject(), getDataStore());
    }
}

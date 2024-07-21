package lakehouse.api.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.List;

/*configtype: dataSet
key: mytabledataSet
DataStore: mydb
endPoint: mytabs.mytable
project: mysecondproject
dependencies:
 - name: otherTable
 - name: anotherTable
description: "my first example mart"
columnSchema:
  - name: id
    type: bigint
    nullable: true
    comment:
  - name: tab_value
    type: string
    nullable: true
    comment:
keys:
  - name: mytable_pk
    type: primary
    runtimeLevelCheck: false
    constructLevelCheck: true*/

@Entity
public class DataSet extends KeyEntityAbstract {

    @ManyToOne
    @JoinColumn(name = "project_key", referencedColumnName = "key")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "data_store_key", referencedColumnName = "key")
    private DataStore dataStore;

    @ElementCollection
    private List<String> sources;

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

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
        this.sources = sources;
    }

}

package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

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
    private Project project;

    @ManyToOne
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



}

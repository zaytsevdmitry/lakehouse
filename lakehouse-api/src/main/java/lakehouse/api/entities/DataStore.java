package lakehouse.api.entities;

import jakarta.persistence.Entity;

import java.util.Objects;

/*configtype: DataStore
key: mydb
interfaceType: jdbc
vendor: postgres
properties:
  url: jdbc:postgres:@localhost
  user: user1
  password: "****"
comment: */
@Entity
public class DataStore extends KeyEntityAbstract {

    private String interfaceType;

    private String vendor;


    public DataStore() {
    }


    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }


    @Override
    public boolean equals(Object o) {

        DataStore DataStore = (DataStore) o;
        return super.equals(o)
                && Objects.equals(getInterfaceType(), DataStore.getInterfaceType())
                && Objects.equals(getVendor(), DataStore.getVendor());
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getInterfaceType(), getVendor());
    }
}

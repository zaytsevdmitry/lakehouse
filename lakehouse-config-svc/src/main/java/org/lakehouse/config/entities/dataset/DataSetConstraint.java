package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "data_set_constraint__uk", columnNames = {
        "data_set_key_name", "name"}))
public class DataSetConstraint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_constraint__data_set_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSet dataSet;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Types.Constraint type;

    @Column(nullable = false)
    private String columns;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean runtimeLevelCheck;

    @Column(nullable = false)
    private boolean constructLevelCheck;

    public DataSetConstraint() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Types.Constraint getType() {
        return type;
    }

    public void setType(Types.Constraint type) {
        this.type = type;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRuntimeLevelCheck() {
        return runtimeLevelCheck;
    }

    public void setRuntimeLevelCheck(boolean runtimeLevelCheck) {
        this.runtimeLevelCheck = runtimeLevelCheck;
    }

    public boolean isConstructLevelCheck() {
        return constructLevelCheck;
    }

    public void setConstructLevelCheck(boolean constructLevelCheck) {

        this.constructLevelCheck = constructLevelCheck;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetConstraint that = (DataSetConstraint) o;
        return isEnabled() == that.isEnabled()
                && isRuntimeLevelCheck() == that.isRuntimeLevelCheck()
                && isConstructLevelCheck() == that.isConstructLevelCheck()
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getDataSet(), that.getDataSet())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getColumns(), that.getColumns());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDataSet(),
                getName(),
                getType(),
                getColumns(),
                isEnabled(),
                isRuntimeLevelCheck(),
                isConstructLevelCheck());
    }
}

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Types.Constraint type;

    @Column(nullable = false)
    private String columns;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Types.ConstraintLevelCheck constraintLevelCheck;

    private String createConstraintDDLOverride;

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

    public Types.ConstraintLevelCheck getConstraintLevelCheck() {
        return constraintLevelCheck;
    }

    public void setConstraintLevelCheck(Types.ConstraintLevelCheck constraintLevelCheck) {
        this.constraintLevelCheck = constraintLevelCheck;
    }

    public String getCreateConstraintDDLOverride() {
        return createConstraintDDLOverride;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetConstraint that = (DataSetConstraint) o;
        return isEnabled() == that.isEnabled() && Objects.equals(getId(), that.getId()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getName(), that.getName()) && getType() == that.getType() && Objects.equals(getColumns(), that.getColumns()) && getConstraintLevelCheck() == that.getConstraintLevelCheck() && Objects.equals(getCreateConstraintDDLOverride(), that.getCreateConstraintDDLOverride());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getDataSet(), getName(), getType(), getColumns(), isEnabled(), getConstraintLevelCheck(), getCreateConstraintDDLOverride());
    }

    public void setCreateConstraintDDLOverride(String createConstraintDDLOverride) {
        this.createConstraintDDLOverride = createConstraintDDLOverride;
    }

    @Override
    public String toString() {
        return "DataSetConstraint{" +
                "id=" + id +
                ", dataSet=" + dataSet +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", columns='" + columns + '\'' +
                ", enabled=" + enabled +
                ", constraintLevelCheck=" + constraintLevelCheck +
                ", createConstraintDDLOverride='" + createConstraintDDLOverride + '\'' +
                '}';
    }
}

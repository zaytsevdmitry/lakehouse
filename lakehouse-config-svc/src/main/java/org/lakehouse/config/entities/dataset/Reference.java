package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Types;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "reference__uk", columnNames = {
        "data_set_constraint_id", "ref_data_set_constraint_id"}))
public class Reference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_reference__data_set_constraint_fk"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DataSetConstraint dataSetConstraint;

    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "data_set_reference__ref_data_set_constraint__fk"))
    private DataSetConstraint refDataSetConstraint;

    private Types.ReferenceAction onDelete;
    private Types.ReferenceAction onUpdate;

    public Reference() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataSetConstraint getDataSetConstraint() {
        return dataSetConstraint;
    }

    public void setDataSetConstraint(DataSetConstraint dataSetConstraint) {
        this.dataSetConstraint = dataSetConstraint;
    }

    public void setRefDataSetConstraint(DataSetConstraint refDataSetConstraint) {
        this.refDataSetConstraint = refDataSetConstraint;
    }

    public DataSetConstraint getRefDataSetConstraint() {
        return refDataSetConstraint;
    }

    public Types.ReferenceAction getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(Types.ReferenceAction onDelete) {
        this.onDelete = onDelete;
    }

    public Types.ReferenceAction getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(Types.ReferenceAction onUpdate) {
        this.onUpdate = onUpdate;
    }
}

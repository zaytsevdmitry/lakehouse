/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.config.entities.dataset;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.lakehouse.client.api.constant.Types;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "foreign_key_reference__uk", columnNames = {
        "data_set_constraint_id", "ref_data_set_constraint_id"}))
public class ForeignKeyReference {
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

    public ForeignKeyReference() {
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

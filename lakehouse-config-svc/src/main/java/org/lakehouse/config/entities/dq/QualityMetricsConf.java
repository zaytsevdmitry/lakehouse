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

package org.lakehouse.config.entities.dq;

import jakarta.persistence.*;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.config.entities.KeyEntityAbstract;
import org.lakehouse.config.entities.dataset.DataSet;

import java.util.Objects;

@Entity
public class QualityMetricsConf extends KeyEntityAbstract {


    @ManyToOne(optional = false)
    @JoinColumn(foreignKey = @ForeignKey(name = "quality_metrics_conf__data_set__fk"))
    private DataSet dataSet;
    @Column(nullable = false, unique = true) private String keyName;
    @Column(nullable = true) private String description;
    @Column(nullable = false) private Types.DQThresholdViolationLevel dqThresholdViolationLevel;
    @Column(nullable = false) private boolean enabled;
    @Column(nullable = false) private boolean save;
    public QualityMetricsConf() {
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getKeyName() {
        return keyName;
    }

    @Override
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    public Types.DQThresholdViolationLevel getDqThresholdViolationLevel() {
        return dqThresholdViolationLevel;
    }

    public boolean isSave() {
        return save;
    }

    public void setSave(boolean save) {
        this.save = save;
    }

    public void setDqThresholdViolationLevel(Types.DQThresholdViolationLevel dqThresholdViolationLevel) {
        this.dqThresholdViolationLevel = dqThresholdViolationLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        QualityMetricsConf that = (QualityMetricsConf) o;
        return isEnabled() == that.isEnabled()
                && isSave() == that.isSave()
                && Objects.equals(getDataSet(), that.getDataSet())
                && Objects.equals(getKeyName(), that.getKeyName())
                && Objects.equals(getDescription(), that.getDescription())
                && getDqThresholdViolationLevel() == that.getDqThresholdViolationLevel();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSet(), getKeyName(), getDescription(), getDqThresholdViolationLevel(), isEnabled(), isSave());
    }
}

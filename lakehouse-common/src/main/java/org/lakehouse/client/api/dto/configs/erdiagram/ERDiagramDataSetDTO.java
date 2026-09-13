/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.client.api.dto.configs.erdiagram;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single data set placed on an ER diagram canvas together with the flow
 * coordinates of its node.
 */
public class ERDiagramDataSetDTO implements Serializable {

    private String keyName;
    private Double x;
    private Double y;

    public ERDiagramDataSetDTO() {
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ERDiagramDataSetDTO that = (ERDiagramDataSetDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName())
                && Objects.equals(getX(), that.getX())
                && Objects.equals(getY(), that.getY());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getX(), getY());
    }

    @Override
    public String toString() {
        return "ERDiagramDataSetDTO{" +
                "keyName='" + keyName + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
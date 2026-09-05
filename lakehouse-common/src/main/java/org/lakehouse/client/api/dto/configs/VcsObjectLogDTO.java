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

package org.lakehouse.client.api.dto.configs;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Per-object outcome of a single configuration repository synchronization cycle.
 */
public class VcsObjectLogDTO implements Serializable {
    private static final long serialVersionUID = 3836548906542997387L;

    private Long id;
    private OffsetDateTime dateTimeRec;
    private String objectName;
    private String kind;
    private String filePath;
    private String commitId;

    public VcsObjectLogDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getDateTimeRec() {
        return dateTimeRec;
    }

    public void setDateTimeRec(OffsetDateTime dateTimeRec) {
        this.dateTimeRec = dateTimeRec;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VcsObjectLogDTO that = (VcsObjectLogDTO) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "VcsObjectLogDTO{" +
                "id=" + id +
                ", dateTimeRec=" + dateTimeRec +
                ", objectName='" + objectName + '\'' +
                ", kind='" + kind + '\'' +
                ", filePath='" + filePath + '\'' +
                ", commitId='" + commitId + '\'' +
                '}';
    }
}
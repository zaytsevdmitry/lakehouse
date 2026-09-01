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

package org.lakehouse.config.vcs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Stores the per-object outcome of a configuration repository synchronization cycle:
 * every configuration construct applied to or un-managed from the database.
 */
@Entity
@Table(name = "vcs_object_log")
public class VcsObjectLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private OffsetDateTime dateTimeRec;

    @Column(nullable = false, length = 256)
    private String objectName;

    @Column(nullable = false, length = 64)
    private String kind;

    @Column(nullable = false, length = 2048)
    private String filePath;

    @Column(nullable = false, length = 64)
    private String commitId;

    public VcsObjectLog() {
    }

    public VcsObjectLog(
            OffsetDateTime dateTimeRec,
            String objectName,
            String kind,
            String filePath,
            String commitId) {
        this.dateTimeRec = dateTimeRec;
        this.objectName = objectName;
        this.kind = kind;
        this.filePath = filePath;
        this.commitId = commitId;
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
        VcsObjectLog that = (VcsObjectLog) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "VcsObjectLog{" +
                "id=" + id +
                ", dateTimeRec=" + dateTimeRec +
                ", objectName='" + objectName + '\'' +
                ", kind='" + kind + '\'' +
                ", filePath='" + filePath + '\'' +
                ", commitId='" + commitId + '\'' +
                '}';
    }
}
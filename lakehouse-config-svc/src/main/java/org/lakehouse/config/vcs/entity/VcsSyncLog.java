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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Tracks the synchronization state of a configuration repository commit.
 * <p>
 * A SUCCESS row stores the commit id already applied to the database, so the next
 * cycle can start its diff from it. A FAILED row stores the reason why a commit
 * could not be applied and prevents infinite retries of an invalid configuration.
 */
@Entity
@Table(name = "vcs_sync_log")
public class VcsSyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String commitId;

    @Column(nullable = false)
    private OffsetDateTime syncDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VcsSyncStatus status;

    @Column(length = 4000)
    private String errorMessage;

    public VcsSyncLog() {
    }

    public VcsSyncLog(String commitId, OffsetDateTime syncDateTime, VcsSyncStatus status, String errorMessage) {
        this.commitId = commitId;
        this.syncDateTime = syncDateTime;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public OffsetDateTime getSyncDateTime() {
        return syncDateTime;
    }

    public void setSyncDateTime(OffsetDateTime syncDateTime) {
        this.syncDateTime = syncDateTime;
    }

    public VcsSyncStatus getStatus() {
        return status;
    }

    public void setStatus(VcsSyncStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VcsSyncLog that = (VcsSyncLog) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getCommitId(), that.getCommitId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCommitId());
    }

    @Override
    public String toString() {
        return "VcsSyncLog{" +
                "id=" + id +
                ", commitId='" + commitId + '\'' +
                ", syncDateTime=" + syncDateTime +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
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
 * Synchronization state of a single configuration repository commit.
 */
public class VcsSyncLogDTO implements Serializable {
    private static final long serialVersionUID = 1910878874788163682L;

    private Long id;
    private String commitId;
    private OffsetDateTime syncDateTime;
    private String status;
    private String errorMessage;

    public VcsSyncLogDTO() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
        VcsSyncLogDTO that = (VcsSyncLogDTO) o;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getCommitId(), that.getCommitId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCommitId());
    }

    @Override
    public String toString() {
        return "VcsSyncLogDTO{" +
                "id=" + id +
                ", commitId='" + commitId + '\'' +
                ", syncDateTime=" + syncDateTime +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
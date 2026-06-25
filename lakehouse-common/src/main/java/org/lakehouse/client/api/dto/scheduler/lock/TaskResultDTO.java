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

package org.lakehouse.client.api.dto.scheduler.lock;

import org.lakehouse.client.api.constant.Status;

public class TaskResultDTO {
    Status.Task status;
    String causes;

    public TaskResultDTO() {

    }

    public TaskResultDTO(Status.Task status, String causes) {
        this.status = status;
        this.causes = causes;
    }

    public TaskResultDTO(Status.Task status) {
        this.status = status;
    }

    public Status.Task getStatus() {
        return status;
    }

    public void setStatus(Status.Task status) {
        this.status = status;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }
}

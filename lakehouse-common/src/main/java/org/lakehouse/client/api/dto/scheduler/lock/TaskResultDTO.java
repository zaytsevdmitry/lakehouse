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

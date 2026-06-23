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

package org.lakehouse.scheduler.exception;


import org.lakehouse.client.api.utils.DateTimeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.OffsetDateTime;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ScheduledNotFoundException extends RuntimeException {
    private static final long serialVersionUID = -613204078471058463L;

    public ScheduledNotFoundException(String keyName, OffsetDateTime targetDateTime) {
        super(String.format("Schedule with name %s and date time %s not found", keyName, DateTimeUtils.formatDateTimeFormatWithTZ(targetDateTime)));
    }
}

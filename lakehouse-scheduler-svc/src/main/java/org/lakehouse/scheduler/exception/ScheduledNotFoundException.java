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

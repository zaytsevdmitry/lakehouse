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

package org.lakehouse.config.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Schedule extends KeyEntityAbstract {

	@Column(nullable = false)
	private String intervalExpression;

	@Column(nullable = false)
	private OffsetDateTime startDateTime;

	private OffsetDateTime endDateTime;

	@Column(nullable = false)
	private boolean enabled;

	@Column(nullable = false)
	private OffsetDateTime lastChangedDateTime;

	@Column(nullable = false)
	private Long lastChangeNumber = 0L;

	public Schedule() {
	}

	public String getIntervalExpression() {
		return intervalExpression;
	}

	public void setIntervalExpression(String intervalExpression) {
		this.intervalExpression = intervalExpression;
	}

	public OffsetDateTime getStartDateTime() {
		return startDateTime;
	}

	public void setStartDateTime(OffsetDateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public OffsetDateTime getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(OffsetDateTime endDateTime) {
		this.endDateTime = endDateTime;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public OffsetDateTime getLastChangedDateTime() {
		return lastChangedDateTime;
	}

	public void setLastChangedDateTime(OffsetDateTime lastChangedDateTime) {
		this.lastChangedDateTime = lastChangedDateTime;
	}

	public Long getLastChangeNumber() {
		return lastChangeNumber;
	}

	public void setLastChangeNumber(Long lastChangeNumber) {
		this.lastChangeNumber = lastChangeNumber;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		Schedule schedule = (Schedule) o;
		return isEnabled() == schedule.isEnabled()
				&& Objects.equals(getIntervalExpression(), schedule.getIntervalExpression())
				&& Objects.equals(getStartDateTime(), schedule.getStartDateTime())
				&& Objects.equals(getLastChangedDateTime(), schedule.getLastChangedDateTime())
				&& Objects.equals(getEndDateTime(), schedule.getEndDateTime())
				&& Objects.equals(getLastChangeNumber(), schedule.getLastChangeNumber());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getIntervalExpression(), getStartDateTime(), getEndDateTime(),
				isEnabled(),getLastChangedDateTime(),getLastChangeNumber());
	}
}

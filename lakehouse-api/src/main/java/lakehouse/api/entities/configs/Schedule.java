package lakehouse.api.entities.configs;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Schedule schedule = (Schedule) o;
        return isEnabled() == schedule.isEnabled()
                && Objects.equals(getIntervalExpression(), schedule.getIntervalExpression()) && Objects.equals(getStartDateTime(), schedule.getStartDateTime()) && Objects.equals(getEndDateTime(), schedule.getEndDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIntervalExpression(), getStartDateTime(), getEndDateTime(), isEnabled());
    }
}

package com.orangeandbronze.enlistment.domain;

import javax.persistence.*;
import java.time.*;

import static org.apache.commons.lang3.Validate.*;

@Embeddable
public class Period {

    private final LocalTime startTime;
    private final LocalTime endTime;

    public Period(LocalTime startTime, LocalTime endTime) {
        notNull(startTime);
        notNull(endTime);
        isTrue(startTime.isBefore(endTime),
                "startTime must be before endTime; startTime: " + startTime + " endTime: " + endTime);
        isTrue(!startTime.isBefore(LocalTime.of(8, 30)), "startTime can't be before 8:30am, was: " + startTime);
        isTrue(!endTime.isAfter(LocalTime.of(17, 30)), "endTime can't be after 5:30pm, was " + endTime);
        checkIf30MinIncrement(startTime);
        checkIf30MinIncrement(endTime);
        this.startTime = startTime;
        this.endTime = endTime;
    }

    private void checkIf30MinIncrement(LocalTime time) {
        notNull(time);
        final int minute = time.getMinute();
        if (minute != 0 && minute != 30) {
            throw new InvalidTimeException("should end with ':00' or ':30', was: " + time);
        }
    }

    void checkEnlistmentOverlap(Period other) {
        if (this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime)) {
            throw new ScheduleConflictException("Period overlap between this: " + this + " & other: " + other);
        }
    }

    boolean checkNewSectionOverlap(Period other) {
        return this.startTime.isBefore(other.endTime) && this.endTime.isAfter(other.startTime);
    }

    @Override
    public String toString() {
        return startTime + " - " + endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Period period = (Period) o;

        if (startTime != null ? !startTime.equals(period.startTime) : period.startTime != null) return false;
        return endTime != null ? endTime.equals(period.endTime) : period.endTime == null;
    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        return result;
    }

    // For JPA only! Do not call!
    private Period() {
        startTime = null;
        endTime = null;
    }
}

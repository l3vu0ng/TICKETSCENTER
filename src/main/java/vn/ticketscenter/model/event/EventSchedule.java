package vn.ticketscenter.model.event;

import java.time.Instant;

public record EventSchedule(Instant saleStart, Instant saleEnd, Instant startTime, Instant endTime) {
    public EventSchedule {
        if (saleStart == null || saleEnd == null || startTime == null || endTime == null
                || !saleStart.isBefore(saleEnd) || saleEnd.isAfter(startTime) || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule must satisfy saleStart < saleEnd <= startTime < endTime");
        }
    }
}

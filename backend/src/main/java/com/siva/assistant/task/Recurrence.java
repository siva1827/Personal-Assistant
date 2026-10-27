package com.siva.assistant.task;

import java.time.LocalDate;

public enum Recurrence {
    NONE, DAILY, WEEKLY, MONTHLY;

    /**
     * The first occurrence after both the finished due date and {@code today}.
     * Monthly steps are counted from the original date, so Jan 31 gives Feb 28 and then Mar 31
     * instead of drifting to the 28th. Returns null for {@link #NONE}.
     */
    public LocalDate nextDue(LocalDate due, LocalDate today) {
        if (this == NONE) {
            return null;
        }
        LocalDate floor = due.isAfter(today) ? due : today;
        int step = 1;
        LocalDate next = advance(due, step);
        while (!next.isAfter(floor)) {
            next = advance(due, ++step);
        }
        return next;
    }

    private LocalDate advance(LocalDate from, int steps) {
        return switch (this) {
            case DAILY -> from.plusDays(steps);
            case WEEKLY -> from.plusWeeks(steps);
            case MONTHLY -> from.plusMonths(steps);
            case NONE -> from;
        };
    }
}

package com.siva.assistant.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RecurrenceTest {

    private static LocalDate d(int y, int m, int day) {
        return LocalDate.of(y, m, day);
    }

    @Test
    void noneHasNoNextOccurrence() {
        assertThat(Recurrence.NONE.nextDue(d(2026, 10, 1), d(2026, 10, 1))).isNull();
    }

    @Test
    void onTimeCompletionMovesOneStep() {
        assertThat(Recurrence.DAILY.nextDue(d(2026, 10, 26), d(2026, 10, 26))).isEqualTo(d(2026, 10, 27));
        assertThat(Recurrence.WEEKLY.nextDue(d(2026, 10, 26), d(2026, 10, 26))).isEqualTo(d(2026, 11, 2));
        assertThat(Recurrence.MONTHLY.nextDue(d(2026, 10, 26), d(2026, 10, 26))).isEqualTo(d(2026, 11, 26));
    }

    @Test
    void completingEarlyDoesNotSkipAnOccurrence() {
        // Finished a day before it was due: the next one is still one step after the due date
        assertThat(Recurrence.WEEKLY.nextDue(d(2026, 10, 28), d(2026, 10, 27))).isEqualTo(d(2026, 11, 4));
    }

    @Test
    void completingLateCatchesUpToTheFuture() {
        // Daily task due 3 days ago, finished today: next is tomorrow, not another overdue copy
        assertThat(Recurrence.DAILY.nextDue(d(2026, 10, 23), d(2026, 10, 26))).isEqualTo(d(2026, 10, 27));
        // Weekly task two weeks late keeps its weekday
        assertThat(Recurrence.WEEKLY.nextDue(d(2026, 10, 5), d(2026, 10, 26))).isEqualTo(d(2026, 11, 2));
    }

    @Test
    void monthlyStepsCountFromTheOriginalDate() {
        // Jan 31 -> Feb 28 (clamped)
        assertThat(Recurrence.MONTHLY.nextDue(d(2026, 1, 31), d(2026, 1, 31))).isEqualTo(d(2026, 2, 28));
        // Finishing the January task in March skips to the next Jan 31 + n months after today: Mar 31
        assertThat(Recurrence.MONTHLY.nextDue(d(2026, 1, 31), d(2026, 3, 5))).isEqualTo(d(2026, 3, 31));
    }

    @Test
    void neverReturnsADateOnOrBeforeToday() {
        for (Recurrence r : new Recurrence[] {Recurrence.DAILY, Recurrence.WEEKLY, Recurrence.MONTHLY}) {
            LocalDate today = d(2026, 10, 26);
            for (int lateDays = -3; lateDays <= 100; lateDays++) {
                LocalDate due = today.minusDays(lateDays);
                assertThat(r.nextDue(due, today)).isAfter(today).isAfter(due);
            }
        }
    }
}

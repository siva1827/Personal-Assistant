package com.siva.assistant.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.task.TaskDtos.CreateTaskRequest;
import com.siva.assistant.task.TaskDtos.UpdateTaskRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final long USER = 7L;
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    @Mock
    TaskRepository tasks;

    TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks, Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    private static UpdateTaskRequest status(TaskStatus status) {
        return new UpdateTaskRequest(null, null, null, null, status);
    }

    @Test
    void createTrimsTitleAndRejectsBlank() {
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(USER, new CreateTaskRequest("  Buy milk ", LocalDate.of(2026, 10, 27), null));

        assertThat(created.title()).isEqualTo("Buy milk");
        assertThat(created.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(created.recurrence()).isEqualTo(Recurrence.NONE);
        assertThatThrownBy(() -> service.create(USER, new CreateTaskRequest("   ", null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void repeatingTaskNeedsADueDate() {
        assertThatThrownBy(() -> service.create(USER, new CreateTaskRequest("Water plants", null, Recurrence.WEEKLY)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(tasks, never()).save(any());
    }

    @Test
    void completingRecordsTimeAndReopeningClearsIt() {
        Task task = new Task(USER, "Call plumber", null);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        var done = service.update(USER, 5L, status(TaskStatus.DONE));
        assertThat(done.status()).isEqualTo(TaskStatus.DONE);
        assertThat(done.completedAt()).isEqualTo(NOW);

        var reopened = service.update(USER, 5L, status(TaskStatus.OPEN));
        assertThat(reopened.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    void completingARepeatingTaskCreatesTheNextOccurrence() {
        Task task = new Task(USER, "Pay rent", LocalDate.of(2026, 10, 1), Recurrence.MONTHLY);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        service.update(USER, 5L, status(TaskStatus.DONE));

        ArgumentCaptor<Task> saved = forClass(Task.class);
        verify(tasks).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Pay rent");
        assertThat(saved.getValue().getDueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(saved.getValue().getRecurrence()).isEqualTo(Recurrence.MONTHLY);
        assertThat(saved.getValue().getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void completingTwiceDoesNotCreateTwoCopies() {
        Task task = new Task(USER, "Stretch", TODAY, Recurrence.DAILY);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        service.update(USER, 5L, status(TaskStatus.DONE));
        service.update(USER, 5L, status(TaskStatus.DONE));

        verify(tasks, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void completingAOneOffTaskCreatesNothing() {
        Task task = new Task(USER, "One off", TODAY);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        service.update(USER, 5L, status(TaskStatus.DONE));

        verify(tasks, never()).save(any());
    }

    @Test
    void completingTwiceKeepsTheOriginalCompletionTime() {
        Task task = new Task(USER, "Pay bill", null);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));
        var first = service.update(USER, 5L, status(TaskStatus.DONE));
        var again = service.update(USER, 5L, status(TaskStatus.DONE));

        assertThat(again.completedAt()).isEqualTo(first.completedAt());
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndCanClearDueDate() {
        Task task = new Task(USER, "Original", LocalDate.of(2026, 10, 30));
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        service.update(USER, 5L, new UpdateTaskRequest("Renamed", null, null, null, null));
        assertThat(task.getTitle()).isEqualTo("Renamed");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 30));

        service.update(USER, 5L, new UpdateTaskRequest(null, null, true, null, null));
        assertThat(task.getDueDate()).isNull();
    }

    @Test
    void removingTheDueDateOfARepeatingTaskIsRejected() {
        Task task = new Task(USER, "Water plants", TODAY, Recurrence.WEEKLY);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.update(USER, 5L, new UpdateTaskRequest(null, null, true, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void listUsesTheRightQueryPerStatus() {
        when(tasks.findOpen(USER)).thenReturn(List.of(new Task(USER, "a", null)));

        assertThat(service.list(USER, TaskStatus.OPEN)).hasSize(1);
        assertThat(service.list(USER, TaskStatus.DONE)).isEmpty();
    }

    @Test
    void otherUsersTaskIsNotFound() {
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(USER, 5L)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(tasks, never()).delete(any());
    }
}

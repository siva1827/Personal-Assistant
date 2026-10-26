package com.siva.assistant.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.task.TaskDtos.CreateTaskRequest;
import com.siva.assistant.task.TaskDtos.UpdateTaskRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final long USER = 7L;
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    @Mock
    TaskRepository tasks;

    TaskService service;

    @BeforeEach
    void setUp() {
        service = new TaskService(tasks);
    }

    @Test
    void createTrimsTitleAndRejectsBlank() {
        when(tasks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var created = service.create(USER, new CreateTaskRequest("  Buy milk ", LocalDate.of(2026, 10, 27)));

        assertThat(created.title()).isEqualTo("Buy milk");
        assertThat(created.status()).isEqualTo(TaskStatus.OPEN);
        assertThatThrownBy(() -> service.create(USER, new CreateTaskRequest("   ", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(tasks, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void completingRecordsTimeAndReopeningClearsIt() {
        Task task = new Task(USER, "Call plumber", null);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        var done = service.update(USER, 5L, new UpdateTaskRequest(null, null, null, TaskStatus.DONE), NOW);
        assertThat(done.status()).isEqualTo(TaskStatus.DONE);
        assertThat(done.completedAt()).isEqualTo(NOW);

        var reopened = service.update(USER, 5L, new UpdateTaskRequest(null, null, null, TaskStatus.OPEN), NOW);
        assertThat(reopened.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(reopened.completedAt()).isNull();
    }

    @Test
    void completingTwiceKeepsTheOriginalCompletionTime() {
        Task task = new Task(USER, "Pay bill", null);
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));
        Instant later = NOW.plusSeconds(3600);

        service.update(USER, 5L, new UpdateTaskRequest(null, null, null, TaskStatus.DONE), NOW);
        var again = service.update(USER, 5L, new UpdateTaskRequest(null, null, null, TaskStatus.DONE), later);

        assertThat(again.completedAt()).isEqualTo(NOW);
    }

    @Test
    void updateChangesOnlyProvidedFieldsAndCanClearDueDate() {
        Task task = new Task(USER, "Original", LocalDate.of(2026, 10, 30));
        when(tasks.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(task));

        service.update(USER, 5L, new UpdateTaskRequest("Renamed", null, null, null), NOW);
        assertThat(task.getTitle()).isEqualTo("Renamed");
        assertThat(task.getDueDate()).isEqualTo(LocalDate.of(2026, 10, 30));

        service.update(USER, 5L, new UpdateTaskRequest(null, null, true, null), NOW);
        assertThat(task.getDueDate()).isNull();
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

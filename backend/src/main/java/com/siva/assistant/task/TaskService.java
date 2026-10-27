package com.siva.assistant.task;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.task.TaskDtos.CreateTaskRequest;
import com.siva.assistant.task.TaskDtos.TaskResponse;
import com.siva.assistant.task.TaskDtos.UpdateTaskRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    /** Only the most recent finished tasks are listed. */
    static final int DONE_LIMIT = 50;

    private final TaskRepository tasks;
    private final Clock clock;

    public TaskService(TaskRepository tasks, Clock clock) {
        this.tasks = tasks;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(long userId, TaskStatus status) {
        List<Task> found = status == TaskStatus.DONE
                ? tasks.findByUserIdAndStatusOrderByCompletedAtDescIdDesc(userId, TaskStatus.DONE,
                        PageRequest.of(0, DONE_LIMIT))
                : tasks.findOpen(userId);
        return found.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(long userId, CreateTaskRequest request) {
        String title = cleanTitle(request.title());
        Recurrence recurrence = request.recurrence() == null ? Recurrence.NONE : request.recurrence();
        requireDueDateForRecurrence(recurrence, request.dueDate());
        return TaskResponse.from(tasks.save(new Task(userId, title, request.dueDate(), recurrence)));
    }

    /**
     * Completing a repeating task also creates its next occurrence, so the series carries on.
     * Completing an already finished task changes nothing, so a double tap cannot add two copies.
     */
    @Transactional
    public TaskResponse update(long userId, long taskId, UpdateTaskRequest request) {
        Task task = find(userId, taskId);
        if (request.title() != null) {
            task.setTitle(cleanTitle(request.title()));
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) {
            task.setDueDate(null);
        } else if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.recurrence() != null) {
            task.setRecurrence(request.recurrence());
        }
        requireDueDateForRecurrence(task.getRecurrence(), task.getDueDate());

        if (request.status() != null) {
            boolean completing = request.status() == TaskStatus.DONE && task.getStatus() == TaskStatus.OPEN;
            task.setStatus(request.status(), Instant.now(clock));
            if (completing) {
                createNextOccurrence(task);
            }
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(long userId, long taskId) {
        tasks.delete(find(userId, taskId));
    }

    private void createNextOccurrence(Task done) {
        if (done.getRecurrence() == Recurrence.NONE || done.getDueDate() == null) {
            return;
        }
        LocalDate next = done.getRecurrence().nextDue(done.getDueDate(), LocalDate.now(clock));
        tasks.save(new Task(done.getUserId(), done.getTitle(), next, done.getRecurrence()));
    }

    private static void requireDueDateForRecurrence(Recurrence recurrence, LocalDate dueDate) {
        if (recurrence != Recurrence.NONE && dueDate == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A repeating task needs a due date");
        }
    }

    private static String cleanTitle(String raw) {
        String title = raw.trim();
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title must not be blank");
        }
        return title;
    }

    private Task find(long userId, long taskId) {
        // Scoped to the user so other users' tasks look like they do not exist
        return tasks.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}

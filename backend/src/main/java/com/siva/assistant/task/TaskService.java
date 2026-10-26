package com.siva.assistant.task;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.task.TaskDtos.CreateTaskRequest;
import com.siva.assistant.task.TaskDtos.TaskResponse;
import com.siva.assistant.task.TaskDtos.UpdateTaskRequest;
import java.time.Instant;
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

    public TaskService(TaskRepository tasks) {
        this.tasks = tasks;
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
        String title = request.title().trim();
        if (title.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "title must not be blank");
        }
        return TaskResponse.from(tasks.save(new Task(userId, title, request.dueDate())));
    }

    @Transactional
    public TaskResponse update(long userId, long taskId, UpdateTaskRequest request, Instant now) {
        Task task = find(userId, taskId);
        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "title must not be blank");
            }
            task.setTitle(title);
        }
        if (Boolean.TRUE.equals(request.clearDueDate())) {
            task.setDueDate(null);
        } else if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.status() != null) {
            task.setStatus(request.status(), now);
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(long userId, long taskId) {
        tasks.delete(find(userId, taskId));
    }

    private Task find(long userId, long taskId) {
        // Scoped to the user so other users' tasks look like they do not exist
        return tasks.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Task not found"));
    }
}

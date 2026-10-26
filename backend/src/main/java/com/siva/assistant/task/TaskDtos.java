package com.siva.assistant.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

public final class TaskDtos {

    private TaskDtos() {
    }

    public record CreateTaskRequest(@NotBlank @Size(max = 200) String title, LocalDate dueDate) {
    }

    /** Fields left null are not changed; set clearDueDate to remove the due date. */
    public record UpdateTaskRequest(
            @Size(min = 1, max = 200) String title,
            LocalDate dueDate,
            Boolean clearDueDate,
            TaskStatus status) {
    }

    public record TaskResponse(
            Long id,
            String title,
            LocalDate dueDate,
            TaskStatus status,
            Long linkedRecipeId,
            Instant completedAt) {

        static TaskResponse from(Task t) {
            return new TaskResponse(t.getId(), t.getTitle(), t.getDueDate(), t.getStatus(),
                    t.getLinkedRecipeId(), t.getCompletedAt());
        }
    }
}

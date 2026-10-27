package com.siva.assistant.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recurrence recurrence = Recurrence.NONE;

    @Column(name = "reminder_at")
    private Instant reminderAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "linked_recipe_id")
    private Long linkedRecipeId;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Task() {
    }

    public Task(Long userId, String title, LocalDate dueDate) {
        this(userId, title, dueDate, Recurrence.NONE);
    }

    public Task(Long userId, String title, LocalDate dueDate, Recurrence recurrence) {
        this.userId = userId;
        this.title = title;
        this.dueDate = dueDate;
        this.recurrence = recurrence;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Recurrence getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(Recurrence recurrence) {
        this.recurrence = recurrence;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Long getLinkedRecipeId() {
        return linkedRecipeId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /** Marking done records when; reopening clears it. */
    public void setStatus(TaskStatus status, Instant now) {
        if (status == this.status) {
            return;
        }
        this.status = status;
        this.completedAt = status == TaskStatus.DONE ? now : null;
    }
}

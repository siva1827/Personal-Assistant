package com.siva.assistant.task;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    /** Soonest due date first; tasks without a due date go last. */
    @Query("""
            select t from Task t
            where t.userId = :userId and t.status = com.siva.assistant.task.TaskStatus.OPEN
            order by case when t.dueDate is null then 1 else 0 end, t.dueDate, t.id
            """)
    List<Task> findOpen(@Param("userId") Long userId);

    /** Open tasks due on or before the date, for reminders. */
    List<Task> findByUserIdAndStatusAndDueDateLessThanEqual(Long userId, TaskStatus status, java.time.LocalDate date);

    List<Task> findByUserIdAndStatusOrderByCompletedAtDescIdDesc(Long userId, TaskStatus status, Pageable pageable);
}

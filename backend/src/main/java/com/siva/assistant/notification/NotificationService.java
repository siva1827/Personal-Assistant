package com.siva.assistant.notification;

import com.siva.assistant.common.ApiException;
import com.siva.assistant.expense.ExpenseDtos.BudgetStatus;
import com.siva.assistant.expense.ExpenseDtos.CategorySummary;
import com.siva.assistant.expense.SpendingSummaryService;
import com.siva.assistant.notification.NotificationDtos.NotificationFeed;
import com.siva.assistant.notification.NotificationDtos.NotificationResponse;
import com.siva.assistant.pantry.PantryItem;
import com.siva.assistant.pantry.PantryItemRepository;
import com.siva.assistant.task.Task;
import com.siva.assistant.task.TaskRepository;
import com.siva.assistant.task.TaskStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    /** Pantry items are flagged this many days before they expire. */
    static final int EXPIRY_WARNING_DAYS = 2;

    private final NotificationRepository notifications;
    private final TaskRepository tasks;
    private final PantryItemRepository pantry;
    private final SpendingSummaryService spending;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, TaskRepository tasks,
                               PantryItemRepository pantry, SpendingSummaryService spending, Clock clock) {
        this.notifications = notifications;
        this.tasks = tasks;
        this.pantry = pantry;
        this.spending = spending;
        this.clock = clock;
    }

    /** Brings the feed up to date first, so it is current even between scheduled runs. */
    @Transactional
    public NotificationFeed feed(long userId) {
        generateFor(userId);
        var items = notifications.findTop50ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(NotificationResponse::from).toList();
        return new NotificationFeed(notifications.countByUserIdAndReadAtIsNull(userId), items);
    }

    @Transactional
    public void markRead(long userId, long notificationId) {
        notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"))
                .markRead(Instant.now(clock));
    }

    @Transactional
    public int markAllRead(long userId) {
        return notifications.markAllRead(userId, Instant.now(clock));
    }

    /**
     * Creates the reminders that apply right now. Safe to call repeatedly: each event has a
     * dedupe key, so it is only ever announced once. Returns how many new ones were created.
     */
    @Transactional
    public int generateFor(long userId) {
        LocalDate today = LocalDate.now(clock);
        int created = 0;
        created += taskReminders(userId, today);
        created += budgetAlerts(userId, YearMonth.from(today));
        created += pantryWarnings(userId, today);
        return created;
    }

    private int taskReminders(long userId, LocalDate today) {
        int created = 0;
        for (Task task : tasks.findByUserIdAndStatusAndDueDateLessThanEqual(userId, TaskStatus.OPEN, today)) {
            boolean dueToday = task.getDueDate().isEqual(today);
            created += create(userId,
                    dueToday ? NotificationType.TASK_DUE : NotificationType.TASK_OVERDUE,
                    dueToday ? "Due today" : "Overdue",
                    dueToday ? task.getTitle() : task.getTitle() + " was due " + task.getDueDate(),
                    "/tasks",
                    // due-today repeats daily-unique; overdue is announced once per task
                    dueToday ? "TASK_DUE:" + task.getId() + ":" + today : "TASK_OVERDUE:" + task.getId());
        }
        return created;
    }

    private int budgetAlerts(long userId, YearMonth month) {
        int created = 0;
        for (CategorySummary row : spending.summary(userId, month).byCategory()) {
            if (row.status() == BudgetStatus.OVER) {
                created += create(userId, NotificationType.BUDGET_OVER, "Over budget",
                        row.name() + " is over its " + month + " budget", "/expenses",
                        "BUDGET_OVER:" + row.categoryId() + ":" + month);
            } else if (row.status() == BudgetStatus.NEAR) {
                created += create(userId, NotificationType.BUDGET_NEAR, "Close to your budget",
                        row.name() + " has used 80% of its " + month + " budget", "/expenses",
                        "BUDGET_NEAR:" + row.categoryId() + ":" + month);
            }
        }
        return created;
    }

    private int pantryWarnings(long userId, LocalDate today) {
        int created = 0;
        var items = pantry.findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDate(
                userId, today.plusDays(EXPIRY_WARNING_DAYS));
        for (PantryItem item : items) {
            long days = ChronoUnit.DAYS.between(today, item.getExpiryDate());
            String name = item.getIngredient().getName();
            String body = days < 0 ? name + " has expired"
                    : days == 0 ? name + " expires today"
                    : name + " expires in " + days + (days == 1 ? " day" : " days");
            created += create(userId, NotificationType.PANTRY_EXPIRING, "Use it up", body, "/pantry",
                    "PANTRY_EXPIRING:" + item.getId());
        }
        return created;
    }

    private int create(long userId, NotificationType type, String title, String body, String link, String key) {
        if (notifications.existsByUserIdAndDedupeKey(userId, key)) {
            return 0;
        }
        notifications.save(new Notification(userId, type, title, body, link, key));
        return 1;
    }
}

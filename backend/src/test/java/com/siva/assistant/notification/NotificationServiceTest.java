package com.siva.assistant.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.expense.ExpenseDtos.BudgetStatus;
import com.siva.assistant.expense.ExpenseDtos.CategorySummary;
import com.siva.assistant.expense.ExpenseDtos.MonthSummary;
import com.siva.assistant.expense.SpendingSummaryService;
import com.siva.assistant.ingredient.Ingredient;
import com.siva.assistant.pantry.PantryItem;
import com.siva.assistant.pantry.PantryItemRepository;
import com.siva.assistant.task.Task;
import com.siva.assistant.task.TaskRepository;
import com.siva.assistant.task.TaskStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final long USER = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Mock
    NotificationRepository notifications;
    @Mock
    TaskRepository tasks;
    @Mock
    PantryItemRepository pantry;
    @Mock
    SpendingSummaryService spending;

    NotificationService service;
    final Set<String> existingKeys = new java.util.HashSet<>();
    final List<Notification> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T06:00:00Z"), ZoneId.of("UTC"));
        service = new NotificationService(notifications, tasks, pantry, spending, clock);
        lenient().when(notifications.existsByUserIdAndDedupeKey(org.mockito.ArgumentMatchers.eq(USER), anyString()))
                .thenAnswer(inv -> existingKeys.contains(inv.<String>getArgument(1)));
        lenient().when(notifications.save(any())).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            saved.add(n);
            existingKeys.add(n.getDedupeKey());
            return n;
        });
        lenient().when(tasks.findByUserIdAndStatusAndDueDateLessThanEqual(USER, TaskStatus.OPEN, TODAY))
                .thenReturn(List.of());
        lenient().when(spending.summary(USER, MONTH))
                .thenReturn(new MonthSummary(MONTH, BigDecimal.ZERO, List.of(), List.of()));
        lenient().when(pantry.findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDate(USER, TODAY.plusDays(2)))
                .thenReturn(List.of());
    }

    private static Task task(long id, String title, LocalDate due) {
        Task t = new Task(USER, title, due);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    private static PantryItem pantryItem(long id, String name, LocalDate expiry) {
        Ingredient ingredient = new Ingredient();
        ReflectionTestUtils.setField(ingredient, "name", name);
        PantryItem item = new PantryItem(USER, ingredient);
        ReflectionTestUtils.setField(item, "id", id);
        item.setExpiryDate(expiry);
        return item;
    }

    @Test
    void remindsAboutTasksDueTodayAndOverdue() {
        when(tasks.findByUserIdAndStatusAndDueDateLessThanEqual(USER, TaskStatus.OPEN, TODAY))
                .thenReturn(List.of(task(1, "Pay rent", TODAY), task(2, "Renew insurance", TODAY.minusDays(6))));

        int created = service.generateFor(USER);

        assertThat(created).isEqualTo(2);
        assertThat(saved).extracting(Notification::getType)
                .containsExactly(NotificationType.TASK_DUE, NotificationType.TASK_OVERDUE);
        assertThat(saved.get(1).getBody()).isEqualTo("Renew insurance was due 2026-10-20");
        assertThat(saved).allMatch(n -> "/tasks".equals(n.getLink()));
    }

    @Test
    void runningTwiceDoesNotRepeatNotifications() {
        when(tasks.findByUserIdAndStatusAndDueDateLessThanEqual(USER, TaskStatus.OPEN, TODAY))
                .thenReturn(List.of(task(1, "Pay rent", TODAY)));

        assertThat(service.generateFor(USER)).isEqualTo(1);
        assertThat(service.generateFor(USER)).isEqualTo(0);

        verify(notifications, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void alertsWhenABudgetIsNearOrOver() {
        when(spending.summary(USER, MONTH)).thenReturn(new MonthSummary(MONTH, new BigDecimal("500"), List.of(
                new CategorySummary(1L, "Food", "x", new BigDecimal("320"), new BigDecimal("400"), BudgetStatus.NEAR),
                new CategorySummary(2L, "Transport", "x", new BigDecimal("150"), new BigDecimal("100"), BudgetStatus.OVER),
                new CategorySummary(3L, "Bills", "x", new BigDecimal("10"), new BigDecimal("100"), BudgetStatus.OK),
                new CategorySummary(4L, "Other", "x", new BigDecimal("30"), null, null)), List.of()));

        service.generateFor(USER);

        assertThat(saved).extracting(Notification::getType)
                .containsExactly(NotificationType.BUDGET_NEAR, NotificationType.BUDGET_OVER);
        assertThat(saved.get(1).getBody()).isEqualTo("Transport is over its 2026-10 budget");
        assertThat(saved.get(0).getDedupeKey()).isEqualTo("BUDGET_NEAR:1:2026-10");
    }

    @Test
    void warnsAboutPantryItemsThatExpireSoonOrAlreadyExpired() {
        when(pantry.findByUserIdAndExpiryDateLessThanEqualOrderByExpiryDate(USER, TODAY.plusDays(2)))
                .thenReturn(List.of(
                        pantryItem(1, "milk", TODAY.minusDays(1)),
                        pantryItem(2, "curd", TODAY),
                        pantryItem(3, "spinach", TODAY.plusDays(1)),
                        pantryItem(4, "paneer", TODAY.plusDays(2))));

        service.generateFor(USER);

        assertThat(saved).extracting(Notification::getBody).containsExactly(
                "milk has expired", "curd expires today", "spinach expires in 1 day", "paneer expires in 2 days");
        assertThat(saved).allMatch(n -> n.getType() == NotificationType.PANTRY_EXPIRING);
    }

    @Test
    void createsNothingWhenThereIsNothingToSay() {
        assertThat(service.generateFor(USER)).isZero();
        verify(notifications, never()).save(any());
    }

    @Test
    void markReadOnlyAffectsTheOwner() {
        when(notifications.findByIdAndUserId(5L, USER)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.markRead(USER, 5L))
                .isInstanceOf(com.siva.assistant.common.ApiException.class);
    }

    @Test
    void capturesArgumentsPassedToSave() {
        when(tasks.findByUserIdAndStatusAndDueDateLessThanEqual(USER, TaskStatus.OPEN, TODAY))
                .thenReturn(List.of(task(9, "Call mum", TODAY)));

        service.generateFor(USER);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Due today");
        assertThat(captor.getValue().getDedupeKey()).isEqualTo("TASK_DUE:9:2026-10-26");
    }
}

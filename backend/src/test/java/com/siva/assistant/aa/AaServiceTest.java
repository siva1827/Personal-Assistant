package com.siva.assistant.aa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.siva.assistant.aa.AaDtos.ConsentResponse;
import com.siva.assistant.aa.AaDtos.SyncResult;
import com.siva.assistant.aa.AaModel.FinancialData;
import com.siva.assistant.aa.AaModel.TransactionType;
import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.category.CategoryService;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.expense.Expense;
import com.siva.assistant.expense.ExpenseRepository;
import com.siva.assistant.expense.ExpenseSource;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AaServiceTest {

    private static final long USER = 7L;
    private static final Instant NOW = Instant.parse("2026-11-06T04:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);
    private static final AaProperties PROPS = new AaProperties("mock", 30, 90);

    @Mock
    AaConsentRepository consentRepo;
    @Mock
    BankAccountRepository accountRepo;
    @Mock
    ExpenseRepository expenseRepo;
    @Mock
    CategoryService categoryService;
    @Mock
    CategoryRepository categoryRepo;

    private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
    private final MockAaProvider provider = new MockAaProvider(clock);
    private AaService service;

    // In-memory stand-ins for the tables
    private final List<AaConsent> consents = new ArrayList<>();
    private final Map<String, BankAccount> bankAccounts = new HashMap<>();
    private final List<Expense> savedExpenses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = newService(provider, clock);
        lenient().when(consentRepo.save(any())).thenAnswer(inv -> {
            AaConsent c = inv.getArgument(0);
            if (c.getId() == null) {
                ReflectionTestUtils.setField(c, "id", (long) (consents.size() + 1));
                consents.add(c);
            }
            return c;
        });
        lenient().when(consentRepo.findByUserIdOrderByIdDesc(anyLong())).thenAnswer(inv ->
                consents.stream().filter(c -> c.getUserId().equals(inv.getArgument(0)))
                        .sorted((a, b) -> Long.compare(b.getId(), a.getId())).toList());
        lenient().when(consentRepo.findByIdAndUserId(anyLong(), anyLong())).thenAnswer(inv ->
                consents.stream().filter(c -> c.getId().equals(inv.getArgument(0))
                        && c.getUserId().equals(inv.getArgument(1))).findFirst());
        lenient().when(consentRepo.existsByUserIdAndStatusIn(anyLong(), anyCollection())).thenAnswer(inv ->
                consents.stream().anyMatch(c -> c.getUserId().equals(inv.getArgument(0))
                        && inv.<Collection<ConsentStatus>>getArgument(1).contains(c.getStatus())));

        lenient().when(accountRepo.save(any())).thenAnswer(inv -> {
            BankAccount a = inv.getArgument(0);
            if (a.getId() == null) {
                ReflectionTestUtils.setField(a, "id", (long) (bankAccounts.size() + 1));
            }
            bankAccounts.put(a.getConsentId() + "/" + a.getProviderRef(), a);
            return a;
        });
        lenient().when(accountRepo.findByConsentIdOrderByIdAsc(anyLong())).thenAnswer(inv ->
                bankAccounts.values().stream().filter(a -> a.getConsentId().equals(inv.getArgument(0)))
                        .sorted((x, y) -> Long.compare(x.getId(), y.getId())).toList());
        lenient().when(accountRepo.findByConsentIdInOrderByIdAsc(anyCollection())).thenAnswer(inv ->
                bankAccounts.values().stream()
                        .filter(a -> inv.<Collection<Long>>getArgument(0).contains(a.getConsentId()))
                        .sorted((x, y) -> Long.compare(x.getId(), y.getId())).toList());

        lenient().when(expenseRepo.save(any())).thenAnswer(inv -> {
            savedExpenses.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        lenient().when(expenseRepo.findExistingExternalRefs(anyLong(), anyCollection())).thenAnswer(inv ->
                savedExpenses.stream().map(Expense::getExternalRef)
                        .filter(inv.<Collection<String>>getArgument(1)::contains).toList());

        List<Category> categories = new ArrayList<>();
        long id = 1;
        for (String name : List.of("Bills", "Entertainment", "Food", "Groceries", "Health", "Other", "Shopping",
                "Transport")) {
            Category c = new Category(USER, name, "x");
            ReflectionTestUtils.setField(c, "id", id++);
            categories.add(c);
        }
        lenient().when(categoryRepo.findByUserIdOrderByName(USER)).thenReturn(categories);
    }

    private AaService newService(AaProvider p, Clock c) {
        return new AaService(p, PROPS, consentRepo, accountRepo, expenseRepo, categoryService, categoryRepo, c);
    }

    private ConsentResponse approvedConnection() {
        ConsentResponse created = service.create(USER, null);
        return service.decide(USER, created.id(), true);
    }

    private static ApiException expectApi(Runnable action, HttpStatus status) {
        ApiException[] caught = new ApiException[1];
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(status);
            caught[0] = e;
        });
        return caught[0];
    }

    // ---- setup and gatekeeping ----

    @Test
    void reportsWhichProviderIsInUse() {
        assertThat(service.status().configured()).isTrue();
        assertThat(service.status().mock()).isTrue();
        assertThat(service.status().provider()).isEqualTo("mock");
        assertThat(newService(new NoAaProvider(), clock).status().configured()).isFalse();
    }

    @Test
    void everythingAnswers503WhenNoProviderIsConfigured() {
        AaService off = newService(new NoAaProvider(), clock);

        expectApi(() -> off.list(USER), HttpStatus.SERVICE_UNAVAILABLE);
        expectApi(() -> off.create(USER, null), HttpStatus.SERVICE_UNAVAILABLE);
        expectApi(() -> off.sync(USER, 1), HttpStatus.SERVICE_UNAVAILABLE);
        expectApi(() -> off.revoke(USER, 1), HttpStatus.SERVICE_UNAVAILABLE);
        verify(consentRepo, never()).save(any());
    }

    // ---- creating and approving ----

    @Test
    void startsAConnectionThatWaitsForApproval() {
        ConsentResponse created = service.create(USER, null);

        assertThat(created.status()).isEqualTo(ConsentStatus.PENDING);
        assertThat(created.purpose()).isEqualTo("Personal finance tracking");
        assertThat(created.dataFrom()).isEqualTo(TODAY.minusDays(90));
        assertThat(created.dataTo()).isEqualTo(TODAY.plusDays(30));
        assertThat(created.redirectUrl()).isNull();
        assertThat(created.accounts()).isEmpty();
    }

    @Test
    void theHistoryLengthCanBeChosen() {
        assertThat(service.create(USER, 30).dataFrom()).isEqualTo(TODAY.minusDays(30));
    }

    @Test
    void onlyOneConnectionCanBePendingOrActive() {
        service.create(USER, null);

        expectApi(() -> service.create(USER, null), HttpStatus.CONFLICT);
    }

    @Test
    void aNewConnectionIsAllowedOnceTheOldOneEnded() {
        ConsentResponse first = service.create(USER, null);
        service.decide(USER, first.id(), false);

        assertThat(service.create(USER, null).status()).isEqualTo(ConsentStatus.PENDING);
    }

    @Test
    void approvingActivatesItAndStartsTheClock() {
        ConsentResponse approved = approvedConnection();

        assertThat(approved.status()).isEqualTo(ConsentStatus.ACTIVE);
        assertThat(approved.approvedAt()).isEqualTo(NOW);
        assertThat(approved.expiresAt()).isEqualTo(NOW.plus(java.time.Duration.ofDays(30)));
    }

    @Test
    void rejectingEndsItWithoutGivingAccess() {
        ConsentResponse created = service.create(USER, null);

        assertThat(service.decide(USER, created.id(), false).status()).isEqualTo(ConsentStatus.REJECTED);
        expectApi(() -> service.sync(USER, created.id()), HttpStatus.CONFLICT);
    }

    @Test
    void aDecisionCanOnlyBeMadeOnce() {
        ConsentResponse approved = approvedConnection();

        expectApi(() -> service.decide(USER, approved.id(), true), HttpStatus.CONFLICT);
        expectApi(() -> service.decide(USER, approved.id(), false), HttpStatus.CONFLICT);
    }

    @Test
    void simulatedApprovalsAreNotAvailableWithARealProvider() {
        AaProvider real = new NoOpRealProvider();
        AaService realService = newService(real, clock);

        expectApi(() -> realService.decide(USER, 1, true), HttpStatus.NOT_FOUND);
    }

    // ---- importing ----

    @Test
    void cannotSyncBeforeApproval() {
        ConsentResponse created = service.create(USER, null);

        ApiException e = expectApi(() -> service.sync(USER, created.id()), HttpStatus.CONFLICT);
        assertThat(e.getMessage()).contains("not been approved");
        verify(expenseRepo, never()).save(any());
    }

    @Test
    void importsMoneySpentAsBankExpensesAndIgnoresIncomingPayments() {
        ConsentResponse approved = approvedConnection();
        FinancialData expected = provider.fetch(lastRef(), approved.dataFrom(), TODAY);
        long debits = expected.transactions().stream().filter(t -> t.type() == TransactionType.DEBIT).count();
        long credits = expected.transactions().stream().filter(t -> t.type() == TransactionType.CREDIT).count();

        SyncResult result = service.sync(USER, approved.id());

        assertThat(result.imported()).isEqualTo((int) debits);
        assertThat(result.creditsIgnored()).isEqualTo((int) credits);
        assertThat(result.skipped()).isZero();
        assertThat(credits).isPositive();
        assertThat(savedExpenses).hasSize((int) debits).allSatisfy(e -> {
            assertThat(e.getSource()).isEqualTo(ExpenseSource.BANK_SYNC);
            assertThat(e.getUserId()).isEqualTo(USER);
            assertThat(e.getAmount().signum()).isPositive();
            assertThat(e.getExternalRef()).startsWith("aa:").hasSize(40);
            assertThat(e.getNote()).isNotBlank();
            assertThat(e.getSpentOn()).isBeforeOrEqualTo(TODAY);
        });
    }

    @Test
    void categorisesImportedExpensesFromTheMerchant() {
        ConsentResponse approved = approvedConnection();

        service.sync(USER, approved.id());

        Map<String, String> categoryByNote = new HashMap<>();
        savedExpenses.forEach(e -> categoryByNote.put(e.getNote(), e.getCategory().getName()));
        assertThat(categoryByNote).containsEntry("SWIGGY", "Food").containsEntry("BIGBASKET", "Groceries")
                .containsEntry("ATM withdrawal", "Other");
        assertThat(categoryByNote.get("NETFLIX")).isEqualTo("Entertainment");
        assertThat(categoryByNote.get("BESCOM ELECTRICITY")).isEqualTo("Bills");
    }

    @Test
    void syncingAgainNeverAddsTheSameTransactionTwice() {
        ConsentResponse approved = approvedConnection();

        SyncResult first = service.sync(USER, approved.id());
        SyncResult second = service.sync(USER, approved.id());

        assertThat(first.imported()).isPositive();
        assertThat(second.imported()).isZero();
        assertThat(second.skipped()).isEqualTo(first.imported());
        assertThat(savedExpenses).hasSize(first.imported());
        assertThat(savedExpenses.stream().map(Expense::getExternalRef).collect(Collectors.toSet()))
                .hasSize(savedExpenses.size());
    }

    @Test
    void keepsAccountsWithMaskedNumbersAndFreshBalances() {
        ConsentResponse approved = approvedConnection();

        service.sync(USER, approved.id());
        service.sync(USER, approved.id());

        assertThat(bankAccounts).hasSize(2);
        assertThat(bankAccounts.values()).allSatisfy(a -> {
            assertThat(a.getMaskedNumber()).startsWith("XXXX");
            assertThat(a.getBalance()).isNotNull();
            assertThat(a.getBalanceAt()).isEqualTo(NOW);
        });
        List<ConsentResponse> listed = service.list(USER);
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).accounts()).hasSize(2);
        assertThat(listed.get(0).lastSyncedAt()).isEqualTo(NOW);
    }

    @Test
    void anExpiredConnectionCannotBeSynced() {
        ConsentResponse approved = approvedConnection();
        Clock later = Clock.fixed(NOW.plus(java.time.Duration.ofDays(31)), ZoneId.of("UTC"));
        AaService afterExpiry = newService(provider, later);

        ApiException e = expectApi(() -> afterExpiry.sync(USER, approved.id()), HttpStatus.CONFLICT);

        assertThat(e.getMessage()).contains("expired");
        assertThat(afterExpiry.list(USER).get(0).status()).isEqualTo(ConsentStatus.EXPIRED);
    }

    @Test
    void anExpiredConnectionNoLongerBlocksStartingANewOne() {
        approvedConnection();
        Clock later = Clock.fixed(NOW.plus(java.time.Duration.ofDays(31)), ZoneId.of("UTC"));

        ConsentResponse fresh = newService(provider, later).create(USER, null);

        assertThat(fresh.status()).isEqualTo(ConsentStatus.PENDING);
    }

    // ---- stopping ----

    @Test
    void stoppingASharedConnectionEndsAccessButKeepsTheImportedExpenses() {
        ConsentResponse approved = approvedConnection();
        service.sync(USER, approved.id());
        int imported = savedExpenses.size();

        service.revoke(USER, approved.id());

        assertThat(service.list(USER).get(0).status()).isEqualTo(ConsentStatus.REVOKED);
        assertThat(provider.consentStatus(lastRef())).contains(ConsentStatus.REVOKED);
        expectApi(() -> service.sync(USER, approved.id()), HttpStatus.CONFLICT);
        verify(expenseRepo, never()).delete(any());
        assertThat(savedExpenses).hasSize(imported);
    }

    @Test
    void stoppingTwiceIsHarmless() {
        ConsentResponse approved = approvedConnection();

        service.revoke(USER, approved.id());
        service.revoke(USER, approved.id());

        assertThat(service.list(USER).get(0).status()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    void aPendingRequestCanBeCancelled() {
        ConsentResponse created = service.create(USER, null);

        service.revoke(USER, created.id());

        assertThat(service.list(USER).get(0).status()).isEqualTo(ConsentStatus.REVOKED);
    }

    @Test
    void stillEndsTheConnectionWhenTheProviderCannotBeReached() {
        AaProvider failing = new NoOpRealProvider() {
            @Override
            public void revoke(String providerRef) {
                throw new IllegalStateException("provider down");
            }
        };
        AaService withFailingProvider = newService(failing, clock);
        ConsentResponse created = withFailingProvider.create(USER, null);

        withFailingProvider.revoke(USER, created.id());

        assertThat(consents.get(0).getStatus()).isEqualTo(ConsentStatus.REVOKED);
    }

    // ---- privacy ----

    @Test
    void otherPeopleCannotSeeOrUseAConnection() {
        ConsentResponse mine = approvedConnection();

        expectApi(() -> service.sync(999L, mine.id()), HttpStatus.NOT_FOUND);
        expectApi(() -> service.revoke(999L, mine.id()), HttpStatus.NOT_FOUND);
        expectApi(() -> service.decide(999L, mine.id(), true), HttpStatus.NOT_FOUND);
        assertThat(service.list(999L)).isEmpty();
    }

    // ---- helpers ----

    private String lastRef() {
        return consents.get(consents.size() - 1).getProviderRef();
    }

    /** A provider that behaves like a real one: configured, not a mock, with nothing to say. */
    private static class NoOpRealProvider implements AaProvider {
        @Override
        public String name() {
            return "real";
        }

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public boolean isMock() {
            return false;
        }

        @Override
        public AaModel.CreatedConsent createConsent(AaModel.ConsentSpec spec) {
            return new AaModel.CreatedConsent("real-1", "https://aa.example/approve/real-1");
        }

        @Override
        public Optional<ConsentStatus> consentStatus(String providerRef) {
            return Optional.empty();
        }

        @Override
        public void revoke(String providerRef) {
        }

        @Override
        public FinancialData fetch(String providerRef, LocalDate from, LocalDate to) {
            return new FinancialData(List.of(), List.of());
        }
    }

    @Test
    void aRealProviderRedirectsTheUserToItsOwnApprovalPage() {
        AaService realService = newService(new NoOpRealProvider(), clock);

        ConsentResponse created = realService.create(USER, null);

        assertThat(created.redirectUrl()).isEqualTo("https://aa.example/approve/real-1");
        assertThat(created.status()).isEqualTo(ConsentStatus.PENDING);
        ArgumentCaptor<AaConsent> saved = ArgumentCaptor.forClass(AaConsent.class);
        verify(consentRepo).save(saved.capture());
        assertThat(saved.getValue().getProvider()).isEqualTo("real");
    }
}

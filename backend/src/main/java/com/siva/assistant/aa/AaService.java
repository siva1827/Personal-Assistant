package com.siva.assistant.aa;

import com.siva.assistant.aa.AaDtos.AccountResponse;
import com.siva.assistant.aa.AaDtos.ConsentResponse;
import com.siva.assistant.aa.AaDtos.StatusResponse;
import com.siva.assistant.aa.AaDtos.SyncResult;
import com.siva.assistant.aa.AaModel.Account;
import com.siva.assistant.aa.AaModel.ConsentSpec;
import com.siva.assistant.aa.AaModel.CreatedConsent;
import com.siva.assistant.aa.AaModel.FinancialData;
import com.siva.assistant.aa.AaModel.Transaction;
import com.siva.assistant.aa.AaModel.TransactionType;
import com.siva.assistant.category.Category;
import com.siva.assistant.category.CategoryRepository;
import com.siva.assistant.category.CategoryService;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.expense.Expense;
import com.siva.assistant.expense.ExpenseRepository;
import com.siva.assistant.expense.ExpenseSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bank connect through an Account Aggregator: the user gives consent, the app reads their accounts
 * and transactions, and money that went out becomes expenses marked as coming from the bank.
 */
@Service
public class AaService {

    private static final Logger log = LoggerFactory.getLogger(AaService.class);
    private static final String PURPOSE = "Personal finance tracking";

    private final AaProvider provider;
    private final AaProperties properties;
    private final AaConsentRepository consents;
    private final BankAccountRepository accounts;
    private final ExpenseRepository expenses;
    private final CategoryService categoryService;
    private final CategoryRepository categories;
    private final Clock clock;

    public AaService(AaProvider provider, AaProperties properties, AaConsentRepository consents,
                     BankAccountRepository accounts, ExpenseRepository expenses, CategoryService categoryService,
                     CategoryRepository categories, Clock clock) {
        this.provider = provider;
        this.properties = properties;
        this.consents = consents;
        this.accounts = accounts;
        this.expenses = expenses;
        this.categoryService = categoryService;
        this.categories = categories;
        this.clock = clock;
    }

    public StatusResponse status() {
        return new StatusResponse(provider.isConfigured(), provider.isMock(), provider.name());
    }

    @Transactional
    public List<ConsentResponse> list(long userId) {
        requireConfigured();
        List<AaConsent> mine = consents.findByUserIdOrderByIdDesc(userId);
        mine.forEach(this::refresh);
        Map<Long, List<AccountResponse>> accountsByConsent = accounts
                .findByConsentIdInOrderByIdAsc(mine.stream().map(AaConsent::getId).toList()).stream()
                .collect(Collectors.groupingBy(BankAccount::getConsentId,
                        Collectors.mapping(AccountResponse::from, Collectors.toList())));
        return mine.stream()
                .map(c -> toResponse(c, accountsByConsent.getOrDefault(c.getId(), List.of()), null))
                .toList();
    }

    /** Starts a connection. Only one can be pending or active at a time. */
    @Transactional
    public ConsentResponse create(long userId, Integer historyDays) {
        requireConfigured();
        consents.findByUserIdOrderByIdDesc(userId).forEach(this::refresh);
        if (consents.existsByUserIdAndStatusIn(userId, EnumSet.of(ConsentStatus.PENDING, ConsentStatus.ACTIVE))) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "You already have a bank connection in progress or active. Stop it first to start a new one");
        }
        LocalDate today = LocalDate.now(clock);
        int days = historyDays == null ? properties.historyDays() : historyDays;
        // The range runs to the end of the consent's life, so later syncs can pick up newer transactions
        ConsentSpec spec = new ConsentSpec(PURPOSE, today.minusDays(days), today.plusDays(properties.consentDays()));
        CreatedConsent created = provider.createConsent(spec);

        AaConsent consent = consents.save(new AaConsent(userId, provider.name(), created.providerRef(), spec.purpose(),
                spec.dataFrom(), spec.dataTo()));
        return toResponse(consent, List.of(), created.redirectUrl());
    }

    /** Demo provider only: stands in for the user approving or rejecting inside their AA app. */
    @Transactional
    public ConsentResponse decide(long userId, long consentId, boolean approve) {
        requireConfigured();
        if (!provider.isMock()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Not found");
        }
        AaConsent consent = find(userId, consentId);
        refresh(consent);
        if (consent.getStatus() != ConsentStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "This request has already been " + consent.getStatus().name().toLowerCase());
        }
        provider.simulateDecision(consent.getProviderRef(), approve);
        refresh(consent);
        return toResponse(consent, accountResponses(consent), null);
    }

    /** Reads accounts and transactions and turns new debits into expenses. Safe to repeat. */
    @Transactional
    public SyncResult sync(long userId, long consentId) {
        requireConfigured();
        AaConsent consent = find(userId, consentId);
        refresh(consent);
        if (consent.getStatus() != ConsentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, switch (consent.getStatus()) {
                case PENDING -> "This connection has not been approved yet";
                case EXPIRED -> "This connection has expired. Start a new one";
                case REVOKED -> "This connection was stopped";
                default -> "This connection was rejected";
            });
        }

        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);
        LocalDate to = consent.getDataTo().isAfter(today) ? today : consent.getDataTo();
        FinancialData data = provider.fetch(consent.getProviderRef(), consent.getDataFrom(), to);

        List<AccountResponse> accountResponses = upsertAccounts(consent, data.accounts(), now);
        categoryService.list(userId); // makes sure the default categories exist
        List<Category> userCategories = categories.findByUserIdOrderByName(userId);

        Map<String, Transaction> debits = new LinkedHashMap<>();
        int creditsIgnored = 0;
        for (Transaction t : data.transactions()) {
            if (t.type() == TransactionType.DEBIT) {
                debits.put(fingerprint(t), t);
            } else {
                creditsIgnored++;
            }
        }
        Set<String> already = debits.isEmpty() ? Set.of()
                : new HashSet<>(expenses.findExistingExternalRefs(userId, debits.keySet()));

        int imported = 0;
        int skipped = 0;
        for (Map.Entry<String, Transaction> entry : debits.entrySet()) {
            if (already.contains(entry.getKey())) {
                skipped++;
                continue;
            }
            Transaction t = entry.getValue();
            Category category = CategoryGuesser.choose(t.narration(), userCategories);
            if (category == null) {
                throw new ApiException(HttpStatus.CONFLICT, "Create a spending category first");
            }
            expenses.save(new Expense(userId, category, t.amount(), NarrationReader.note(t.narration()), t.date(),
                    ExpenseSource.BANK_SYNC, entry.getKey()));
            imported++;
        }
        consent.markSynced(now);
        log.info("Bank sync for user {}: {} imported, {} already there, {} credits ignored", userId, imported,
                skipped, creditsIgnored);
        return new SyncResult(imported, skipped, creditsIgnored, accountResponses);
    }

    /** Withdraws permission. Expenses already imported are kept. Stopping twice is harmless. */
    @Transactional
    public void revoke(long userId, long consentId) {
        requireConfigured();
        AaConsent consent = find(userId, consentId);
        if (consent.getStatus() == ConsentStatus.PENDING || consent.getStatus() == ConsentStatus.ACTIVE) {
            try {
                provider.revoke(consent.getProviderRef());
            } catch (RuntimeException e) {
                // The user's decision stands even if the provider could not be told right now
                log.warn("Could not tell the provider about the revoked consent {}", consent.getId(), e);
            }
            consent.moveTo(ConsentStatus.REVOKED);
        }
    }

    // ---- helpers ----

    /** Brings a consent up to date with the provider and the clock. */
    private void refresh(AaConsent consent) {
        if (consent.getStatus() != ConsentStatus.PENDING && consent.getStatus() != ConsentStatus.ACTIVE) {
            return;
        }
        try {
            provider.consentStatus(consent.getProviderRef()).ifPresent(reported -> apply(consent, reported));
        } catch (RuntimeException e) {
            log.warn("Could not check consent {} with the provider", consent.getId(), e);
        }
        if (consent.isPastExpiry(Instant.now(clock))) {
            consent.moveTo(ConsentStatus.EXPIRED);
        }
    }

    private void apply(AaConsent consent, ConsentStatus reported) {
        switch (reported) {
            case ACTIVE -> {
                if (consent.getStatus() == ConsentStatus.PENDING) {
                    consent.activate(Instant.now(clock), Duration.ofDays(properties.consentDays()));
                }
            }
            case REJECTED, REVOKED, EXPIRED -> consent.moveTo(reported);
            case PENDING -> {
            }
        }
    }

    private List<AccountResponse> upsertAccounts(AaConsent consent, List<Account> fetched, Instant now) {
        Map<String, BankAccount> existing = accounts.findByConsentIdOrderByIdAsc(consent.getId()).stream()
                .collect(Collectors.toMap(BankAccount::getProviderRef, Function.identity()));
        for (Account account : fetched) {
            BankAccount saved = existing.get(account.ref());
            if (saved == null) {
                saved = new BankAccount(consent.getUserId(), consent.getId(), account.ref(), account.bankName(),
                        account.maskedNumber(), account.type());
            }
            saved.updateBalance(account.balance(), now);
            accounts.save(saved);
        }
        return accountResponses(consent);
    }

    private List<AccountResponse> accountResponses(AaConsent consent) {
        return accounts.findByConsentIdOrderByIdAsc(consent.getId()).stream().map(AccountResponse::from).toList();
    }

    private ConsentResponse toResponse(AaConsent c, List<AccountResponse> accountList, String redirectUrl) {
        return new ConsentResponse(c.getId(), c.getStatus(), c.getPurpose(), c.getDataFrom(), c.getDataTo(),
                c.getCreatedAt(), c.getApprovedAt(), c.getExpiresAt(), c.getLastSyncedAt(), redirectUrl, accountList);
    }

    private AaConsent find(long userId, long consentId) {
        // Scoped to the user so other people's connections look like they do not exist
        return consents.findByIdAndUserId(consentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bank connection not found"));
    }

    private void requireConfigured() {
        if (!provider.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Bank connect is not set up yet (see docs/BANK_CONNECT.md)");
        }
    }

    /** "aa:" plus a hash of the provider and transaction id: stable, 40 characters, fits the column. */
    String fingerprint(Transaction t) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((provider.name() + "|" + t.id()).getBytes(StandardCharsets.UTF_8));
            return "aa:" + HexFormat.of().formatHex(digest).substring(0, 37);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

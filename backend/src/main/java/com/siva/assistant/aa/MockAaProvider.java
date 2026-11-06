package com.siva.assistant.aa;

import com.siva.assistant.aa.AaModel.Account;
import com.siva.assistant.aa.AaModel.ConsentSpec;
import com.siva.assistant.aa.AaModel.CreatedConsent;
import com.siva.assistant.aa.AaModel.FinancialData;
import com.siva.assistant.aa.AaModel.Transaction;
import com.siva.assistant.aa.AaModel.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pretend Account Aggregator and bank, switched on with AA_PROVIDER=mock. It exists so the whole
 * flow (consent request, approval, data fetch, import) can be tried without a provider account.
 *
 * Everything it returns is invented. The data is generated from the consent reference and the date,
 * so fetching the same day twice gives the same transactions (which lets duplicate protection be
 * tested), and a longer range simply adds more days. It never generates transactions in the future.
 */
public class MockAaProvider implements AaProvider {

    private record Merchant(String name, int min, int max, String mode) {
    }

    private static final List<Merchant> MERCHANTS = List.of(
            new Merchant("SWIGGY", 120, 650, "UPI"),
            new Merchant("ZOMATO", 150, 700, "UPI"),
            new Merchant("UBER INDIA", 80, 450, "UPI"),
            new Merchant("OLA CABS", 70, 380, "UPI"),
            new Merchant("BIGBASKET", 300, 2200, "UPI"),
            new Merchant("BLINKIT", 120, 900, "UPI"),
            new Merchant("AMAZON PAY INDIA", 199, 4500, "POS"),
            new Merchant("FLIPKART", 299, 3500, "POS"),
            new Merchant("APOLLO PHARMACY", 90, 1200, "POS"),
            new Merchant("BOOKMYSHOW", 250, 1100, "UPI"),
            new Merchant("RAMESH KIRANA STORE", 40, 400, "UPI"),
            new Merchant("INDIAN OIL PETROL PUMP", 500, 2500, "POS"));

    private static final List<String> ATM_PLACES = List.of("MG ROAD BRANCH", "RAILWAY STATION", "CITY MALL");

    private final Clock clock;
    private final Map<String, ConsentStatus> decisions = new ConcurrentHashMap<>();

    public MockAaProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public boolean isMock() {
        return true;
    }

    @Override
    public CreatedConsent createConsent(ConsentSpec spec) {
        String ref = "mock-" + UUID.randomUUID();
        decisions.put(ref, ConsentStatus.PENDING);
        return new CreatedConsent(ref, null);
    }

    @Override
    public Optional<ConsentStatus> consentStatus(String providerRef) {
        return Optional.ofNullable(decisions.get(providerRef));
    }

    @Override
    public void revoke(String providerRef) {
        decisions.put(providerRef, ConsentStatus.REVOKED);
    }

    @Override
    public void simulateDecision(String providerRef, boolean approve) {
        decisions.put(providerRef, approve ? ConsentStatus.ACTIVE : ConsentStatus.REJECTED);
    }

    @Override
    public FinancialData fetch(String providerRef, LocalDate from, LocalDate to) {
        LocalDate last = to.isAfter(LocalDate.now(clock)) ? LocalDate.now(clock) : to;
        List<Transaction> transactions = new ArrayList<>();
        BigDecimal[] balances = {new BigDecimal("25000.00"), new BigDecimal("8000.00")};

        for (LocalDate day = from; !day.isAfter(last); day = day.plusDays(1)) {
            for (int account = 0; account < 2; account++) {
                for (Transaction t : transactionsOn(providerRef, account, day)) {
                    transactions.add(t);
                    balances[account] = t.type() == TransactionType.CREDIT
                            ? balances[account].add(t.amount()) : balances[account].subtract(t.amount());
                }
            }
        }
        List<Account> accounts = List.of(
                new Account(providerRef + "-a0", "Demo Bank", "XXXX4321", "SAVINGS", balances[0]),
                new Account(providerRef + "-a1", "Demo Co-operative Bank", "XXXX9087", "SAVINGS", balances[1]));
        return new FinancialData(accounts, transactions);
    }

    private List<Transaction> transactionsOn(String ref, int account, LocalDate day) {
        Random random = new Random(31L * ref.hashCode() + 7919L * account + day.toEpochDay());
        List<Transaction> found = new ArrayList<>();
        String accountRef = ref + "-a" + account;
        int n = 0;

        if (account == 0) {
            if (day.getDayOfMonth() == 1) {
                found.add(txn(accountRef, day, n++, new BigDecimal("52000.00"), TransactionType.CREDIT,
                        "NEFT/CR/" + digits(random) + "/ACME PAYROLL"));
            }
            if (day.getDayOfMonth() == 5) {
                found.add(txn(accountRef, day, n++, new BigDecimal("649.00"), TransactionType.DEBIT,
                        "ACH/NETFLIX"));
            }
            if (day.getDayOfMonth() == 10) {
                found.add(txn(accountRef, day, n++, amount(random, 800, 2200), TransactionType.DEBIT,
                        "UPI/DR/" + digits(random) + "/BESCOM ELECTRICITY/ybl"));
            }
            int spends = random.nextInt(100) < 55 ? (random.nextInt(100) < 25 ? 2 : 1) : 0;
            for (int i = 0; i < spends; i++) {
                Merchant m = MERCHANTS.get(random.nextInt(MERCHANTS.size()));
                String narration = m.mode().equals("UPI")
                        ? "UPI/DR/" + digits(random) + "/" + m.name() + "/okhdfcbank"
                        : "POS/" + m.name();
                found.add(txn(accountRef, day, n++, amount(random, m.min(), m.max()), TransactionType.DEBIT, narration));
            }
        } else {
            // A second, quieter account: topped up monthly, used for the odd cash withdrawal
            if (day.getDayOfMonth() == 2) {
                found.add(txn(accountRef, day, n++, new BigDecimal("15000.00"), TransactionType.CREDIT,
                        "NEFT/CR/" + digits(random) + "/SELF TRANSFER"));
            }
            if (random.nextInt(100) < 12) {
                found.add(txn(accountRef, day, n++, amount(random, 5, 20).multiply(new BigDecimal(100)),
                        TransactionType.DEBIT, "ATM/WDL/" + ATM_PLACES.get(random.nextInt(ATM_PLACES.size()))));
            }
        }
        return found;
    }

    private static Transaction txn(String accountRef, LocalDate day, int n, BigDecimal amount,
                                   TransactionType type, String narration) {
        return new Transaction(accountRef + "-" + day + "-" + n, accountRef, day, amount, type, narration);
    }

    /** Whole rupees, with 50 paise on every third amount, so the data is not suspiciously round. */
    private static BigDecimal amount(Random random, int min, int max) {
        int rupees = min + random.nextInt(max - min + 1);
        return new BigDecimal(rupees).add(random.nextInt(3) == 0 ? new BigDecimal("0.50") : BigDecimal.ZERO)
                .setScale(2);
    }

    private static String digits(Random random) {
        return String.valueOf(100_000_000L + random.nextInt(900_000_000));
    }
}

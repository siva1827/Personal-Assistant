package com.siva.assistant.aa;

import static org.assertj.core.api.Assertions.assertThat;

import com.siva.assistant.aa.AaModel.CreatedConsent;
import com.siva.assistant.aa.AaModel.FinancialData;
import com.siva.assistant.aa.AaModel.Transaction;
import com.siva.assistant.aa.AaModel.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MockAaProviderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);
    private final MockAaProvider provider =
            new MockAaProvider(Clock.fixed(Instant.parse("2026-11-06T04:00:00Z"), ZoneId.of("UTC")));

    private CreatedConsent consent() {
        return provider.createConsent(new AaModel.ConsentSpec("test", TODAY.minusDays(90), TODAY));
    }

    @Test
    void walksThroughTheConsentLifecycle() {
        String ref = consent().providerRef();

        assertThat(provider.consentStatus(ref)).contains(ConsentStatus.PENDING);
        provider.simulateDecision(ref, true);
        assertThat(provider.consentStatus(ref)).contains(ConsentStatus.ACTIVE);
        provider.revoke(ref);
        assertThat(provider.consentStatus(ref)).contains(ConsentStatus.REVOKED);

        String other = consent().providerRef();
        provider.simulateDecision(other, false);
        assertThat(provider.consentStatus(other)).contains(ConsentStatus.REJECTED);
    }

    @Test
    void knowsNothingAboutConsentsItDidNotCreate() {
        assertThat(provider.consentStatus("mock-forgotten-after-restart")).isEqualTo(Optional.empty());
    }

    @Test
    void identifiesItselfAsADemoWithNoRedirect() {
        CreatedConsent created = consent();

        assertThat(provider.isMock()).isTrue();
        assertThat(provider.isConfigured()).isTrue();
        assertThat(provider.name()).isEqualTo("mock");
        assertThat(created.redirectUrl()).isNull();
        assertThat(created.providerRef()).startsWith("mock-");
    }

    @Test
    void givesTheSameTransactionsForTheSameDayEveryTime() {
        String ref = consent().providerRef();

        FinancialData first = provider.fetch(ref, TODAY.minusDays(30), TODAY);
        FinancialData again = provider.fetch(ref, TODAY.minusDays(30), TODAY);

        assertThat(again.transactions()).isEqualTo(first.transactions());
        assertThat(first.transactions()).isNotEmpty();
    }

    @Test
    void aLongerRangeOnlyAddsDaysAndNeverChangesOnesAlreadySeen() {
        String ref = consent().providerRef();

        var short30 = provider.fetch(ref, TODAY.minusDays(30), TODAY).transactions();
        var long90 = provider.fetch(ref, TODAY.minusDays(90), TODAY).transactions();

        assertThat(long90).containsAll(short30);
        assertThat(long90.size()).isGreaterThan(short30.size());
    }

    @Test
    void differentConsentsGetDifferentData() {
        var a = provider.fetch(consent().providerRef(), TODAY.minusDays(60), TODAY).transactions();
        var b = provider.fetch(consent().providerRef(), TODAY.minusDays(60), TODAY).transactions();

        assertThat(a.stream().map(Transaction::amount).toList()).isNotEqualTo(b.stream().map(Transaction::amount).toList());
    }

    @Test
    void neverInventsTransactionsInTheFuture() {
        FinancialData data = provider.fetch(consent().providerRef(), TODAY.minusDays(5), TODAY.plusDays(30));

        assertThat(data.transactions()).allSatisfy(t -> assertThat(t.date()).isBeforeOrEqualTo(TODAY));
    }

    @Test
    void returnsTwoAccountsWithMaskedNumbersOnly() {
        String ref = consent().providerRef();

        FinancialData data = provider.fetch(ref, TODAY.minusDays(10), TODAY);

        assertThat(data.accounts()).hasSize(2);
        assertThat(data.accounts()).allSatisfy(a -> {
            assertThat(a.maskedNumber()).startsWith("XXXX").hasSize(8);
            assertThat(a.ref()).startsWith(ref);
        });
        assertThat(data.transactions()).allSatisfy(t -> assertThat(t.accountRef()).startsWith(ref));
    }

    @Test
    void includesSalaryCreditsAndSpendingDebitsWithSensibleAmounts() {
        var transactions = provider.fetch(consent().providerRef(), TODAY.minusDays(90), TODAY).transactions();

        var credits = transactions.stream().filter(t -> t.type() == TransactionType.CREDIT).toList();
        var debits = transactions.stream().filter(t -> t.type() == TransactionType.DEBIT).toList();
        var payroll = credits.stream().filter(t -> t.narration().contains("PAYROLL")).toList();
        assertThat(payroll).isNotEmpty().allSatisfy(t -> {
            assertThat(t.amount()).isEqualByComparingTo("52000");
            assertThat(t.date().getDayOfMonth()).isEqualTo(1);
        });
        assertThat(credits).anySatisfy(t -> assertThat(t.narration()).contains("SELF TRANSFER"));
        assertThat(debits.size()).isGreaterThan(20);
        assertThat(debits).allSatisfy(t -> {
            assertThat(t.amount().signum()).isPositive();
            assertThat(t.amount().scale()).isEqualTo(2);
        });
    }

    @Test
    void transactionIdsAreUnique() {
        var ids = provider.fetch(consent().providerRef(), TODAY.minusDays(120), TODAY).transactions().stream()
                .map(Transaction::id).collect(Collectors.toList());

        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void savingsBalancesNeverGoNegativeWhateverTheConsent() {
        for (int i = 0; i < 40; i++) {
            FinancialData data = provider.fetch(consent().providerRef(), TODAY.minusDays(90), TODAY);

            assertThat(data.accounts()).allSatisfy(a -> assertThat(a.balance().signum()).isGreaterThanOrEqualTo(0));
        }
    }

    @Test
    void balancesAddUpFromTheStartingAmountAndTheTransactions() {
        String ref = consent().providerRef();
        FinancialData data = provider.fetch(ref, TODAY.minusDays(60), TODAY);

        BigDecimal net = data.transactions().stream().filter(t -> t.accountRef().endsWith("-a0"))
                .map(t -> t.type() == TransactionType.CREDIT ? t.amount() : t.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(data.accounts().get(0).balance()).isEqualByComparingTo(new BigDecimal("25000").add(net));
    }
}

package com.siva.assistant.aa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NarrationReaderTest {

    @Test
    void findsTheMerchantInCommonStatementFormats() {
        assertThat(NarrationReader.merchant("UPI/DR/412345678/SWIGGY/okhdfcbank")).contains("SWIGGY");
        assertThat(NarrationReader.merchant("POS/AMAZON PAY INDIA")).contains("AMAZON PAY INDIA");
        assertThat(NarrationReader.merchant("ACH/NETFLIX")).contains("NETFLIX");
        assertThat(NarrationReader.merchant("NEFT/CR/123456789/ACME PAYROLL")).contains("ACME PAYROLL");
        assertThat(NarrationReader.merchant("UPI/DR/55555/BESCOM ELECTRICITY/ybl")).contains("BESCOM ELECTRICITY");
    }

    @Test
    void skipsPaymentModeWordsNumbersAndUpiHandles() {
        assertThat(NarrationReader.merchant("UPI/DR/123/someone@okicici/dinner")).contains("dinner");
        assertThat(NarrationReader.merchant("IMPS/DR/998877/RAJESH KUMAR")).contains("RAJESH KUMAR");
        assertThat(NarrationReader.merchant("UPI/DR/123/okhdfcbank")).isEmpty();
    }

    @Test
    void handlesMissingOrUnreadableNarrations() {
        assertThat(NarrationReader.merchant(null)).isEmpty();
        assertThat(NarrationReader.merchant("")).isEmpty();
        assertThat(NarrationReader.merchant("///")).isEmpty();
    }

    @Test
    void writesAReadableNoteForTheExpense() {
        assertThat(NarrationReader.note("UPI/DR/412345678/SWIGGY/okhdfcbank")).isEqualTo("SWIGGY");
        assertThat(NarrationReader.note("ATM/WDL/MG ROAD BRANCH")).isEqualTo("ATM withdrawal");
        assertThat(NarrationReader.note("atm/wdl/city mall")).isEqualTo("ATM withdrawal");
        assertThat(NarrationReader.note(null)).isEqualTo("Bank transaction");
        // when no name can be found the raw text is kept, so the user still sees something useful
        assertThat(NarrationReader.note("UPI/DR/123/okhdfcbank")).isEqualTo("UPI/DR/123/okhdfcbank");
    }

    @Test
    void neverReturnsANoteLongerThanTheColumnAllows() {
        assertThat(NarrationReader.note("x".repeat(500))).hasSize(200);
    }
}

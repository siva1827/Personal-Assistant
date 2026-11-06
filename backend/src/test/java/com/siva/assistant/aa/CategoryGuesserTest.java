package com.siva.assistant.aa;

import static org.assertj.core.api.Assertions.assertThat;

import com.siva.assistant.category.Category;
import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryGuesserTest {

    private static final List<Category> CATEGORIES = List.of(
            new Category(1L, "Bills", "x"), new Category(1L, "Entertainment", "x"), new Category(1L, "Food", "x"),
            new Category(1L, "Groceries", "x"), new Category(1L, "Health", "x"), new Category(1L, "Other", "x"),
            new Category(1L, "Shopping", "x"), new Category(1L, "Transport", "x"));

    private static String guess(String text) {
        return CategoryGuesser.choose(text, CATEGORIES).getName();
    }

    @Test
    void matchesMerchantsToTheUsersCategories() {
        assertThat(guess("UPI/DR/123/SWIGGY/okhdfcbank")).isEqualTo("Food");
        assertThat(guess("UPI/DR/123/BIGBASKET/ybl")).isEqualTo("Groceries");
        assertThat(guess("UPI/DR/123/RAMESH KIRANA STORE/ybl")).isEqualTo("Groceries");
        assertThat(guess("UPI/DR/123/UBER INDIA/ybl")).isEqualTo("Transport");
        assertThat(guess("POS/INDIAN OIL PETROL PUMP")).isEqualTo("Transport");
        assertThat(guess("POS/AMAZON PAY INDIA")).isEqualTo("Shopping");
        assertThat(guess("UPI/DR/123/BESCOM ELECTRICITY/ybl")).isEqualTo("Bills");
        assertThat(guess("POS/APOLLO PHARMACY")).isEqualTo("Health");
        assertThat(guess("ACH/NETFLIX")).isEqualTo("Entertainment");
    }

    @Test
    void isNotFussyAboutCase() {
        assertThat(guess("upi/dr/1/swiggy/ok")).isEqualTo("Food");
        assertThat(CategoryGuesser.choose("SWIGGY", List.of(new Category(1L, "FOOD", "x"))).getName()).isEqualTo("FOOD");
    }

    @Test
    void fallsBackToOtherForUnknownMerchantsAndCashWithdrawals() {
        assertThat(guess("UPI/DR/123/SOMEONE NEW/ybl")).isEqualTo("Other");
        assertThat(guess("ATM/WDL/MG ROAD BRANCH")).isEqualTo("Other");
        assertThat(guess(null)).isEqualTo("Other");
    }

    @Test
    void fallsBackToTheFirstCategoryWhenThereIsNoOther() {
        List<Category> custom = List.of(new Category(1L, "Pets", "x"), new Category(1L, "Rent", "x"));

        assertThat(CategoryGuesser.choose("UPI/DR/1/SWIGGY", custom).getName()).isEqualTo("Pets");
    }

    @Test
    void skipsAKeywordGroupTheUserDoesNotHaveACategoryFor() {
        // the user deleted "Food": a Swiggy payment must not be forced into a category that is not there
        List<Category> noFood = List.of(new Category(1L, "Other", "x"), new Category(1L, "Transport", "x"));

        assertThat(CategoryGuesser.choose("UPI/DR/1/SWIGGY", noFood).getName()).isEqualTo("Other");
    }

    @Test
    void returnsNothingOnlyWhenThereAreNoCategoriesAtAll() {
        assertThat(CategoryGuesser.choose("SWIGGY", List.of())).isNull();
    }
}

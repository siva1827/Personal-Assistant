package com.siva.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class IngredientMatcherTest {

    private static final List<String> PANTRY =
            List.of("tomato", "onion", "sweet potato", "green chilli", "coriander leaves", "salt", "cooking oil");

    @Test
    void matchesPluralsQuantitiesAndPreparationWords() {
        assertThat(IngredientMatcher.inPantry("2 ripe tomatoes", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("Onions, finely chopped", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("1 cup chopped tomato", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("salt", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("cooking oil", PANTRY)).isTrue();
    }

    @Test
    void treatsSpellingVariantsAsTheSameIngredient() {
        assertThat(IngredientMatcher.inPantry("green chillies", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("green chilli", PANTRY)).isTrue();
        assertThat(IngredientMatcher.inPantry("coriander leaves", PANTRY)).isTrue();
    }

    @Test
    void doesNotConfuseDifferentIngredientsThatShareAWord() {
        // pantry has "sweet potato" only: plain potato is a different thing
        assertThat(IngredientMatcher.inPantry("potato", PANTRY)).isFalse();
        // pantry has "tomato": ketchup and puree are not it
        assertThat(IngredientMatcher.inPantry("tomato ketchup", PANTRY)).isFalse();
        // and the other way round
        assertThat(IngredientMatcher.inPantry("potato", List.of("potato"))).isTrue();
        assertThat(IngredientMatcher.inPantry("sweet potato", List.of("potato"))).isFalse();
    }

    @Test
    void anIngredientThatIsNotThereIsNotMatched() {
        assertThat(IngredientMatcher.inPantry("cumin seeds", PANTRY)).isFalse();
        assertThat(IngredientMatcher.inPantry("", PANTRY)).isFalse();
        assertThat(IngredientMatcher.inPantry("tomato", List.of())).isFalse();
    }

    @Test
    void findsAllergensIncludingInsideCompoundWords() {
        assertThat(IngredientMatcher.mentionsAllergen("Roasted peanuts", List.of("peanut"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("peanut butter", List.of("peanuts"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("Garnish with PEANUTS", List.of("Peanut"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("sesame seeds", List.of("sesame"))).isTrue();
    }

    @Test
    void generalAllergyNamesCoverTheirCommonMembers() {
        assertThat(IngredientMatcher.mentionsAllergen("12 prawns", List.of("shellfish"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("crab curry", List.of("Shellfish"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("paneer cubes", List.of("dairy"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("a spoon of ghee", List.of("milk"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("walnut halves", List.of("nuts"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("whole wheat flour", List.of("gluten"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("silken tofu", List.of("soy"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("mayonnaise", List.of("egg"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("salmon fillet", List.of("fish"))).isTrue();
    }

    @Test
    void errsOnTheSideOfCautionForShortAllergenNames() {
        // "nut" is found inside "coconut": a false alarm is acceptable, a missed allergen is not
        assertThat(IngredientMatcher.mentionsAllergen("coconut milk", List.of("nut"))).isTrue();
    }

    @Test
    void leavesUnrelatedFoodAlone() {
        assertThat(IngredientMatcher.mentionsAllergen("tomato and onion curry", List.of("peanut", "shellfish")))
                .isFalse();
        assertThat(IngredientMatcher.mentionsAllergen("anything", List.of())).isFalse();
        assertThat(IngredientMatcher.mentionsAllergen("anything", List.of("  ", ""))).isFalse();
        assertThat(IngredientMatcher.mentionsAllergen(null, List.of("peanut"))).isFalse();
    }

    @Test
    void worksWithIndianLanguageNamesWhoseLettersUseCombiningMarks() {
        // "मूंगफली" is peanut, "पनीर" is paneer: the vowel signs must stay inside the words
        assertThat(IngredientMatcher.mentionsAllergen("मूंगफली की चटनी", List.of("मूंगफली"))).isTrue();
        assertThat(IngredientMatcher.mentionsAllergen("टमाटर की सब्ज़ी", List.of("मूंगफली"))).isFalse();
        assertThat(IngredientMatcher.inPantry("2 पनीर", List.of("पनीर"))).isTrue();
        assertThat(IngredientMatcher.inPantry("पनीर", List.of("टमाटर"))).isFalse();
        assertThat(IngredientMatcher.normalized("पनीर")).isEqualTo("पनीर");
    }

    @Test
    void reducesWordsToACommonForm() {
        assertThat(IngredientMatcher.normalized("Tomatoes, Chillies & Berries!")).isEqualTo("tomato chilly berry");
        assertThat(IngredientMatcher.singular("glass")).isEqualTo("glass");
        assertThat(IngredientMatcher.singular("egg")).isEqualTo("egg");
    }
}

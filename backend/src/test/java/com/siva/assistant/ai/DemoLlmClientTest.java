package com.siva.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.siva.assistant.ai.RecipeDrafts.RecipeBatch;
import com.siva.assistant.user.DietaryPreference;
import jakarta.validation.Validation;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class DemoLlmClientTest {

    private final DemoLlmClient demo = new DemoLlmClient(Jackson2ObjectMapperBuilder.json().build());
    private final PromptBuilder prompts = new PromptBuilder();
    private final RecipeResponseParser parser = new RecipeResponseParser(
            Jackson2ObjectMapperBuilder.json().build(), Validation.buildDefaultValidatorFactory().getValidator());

    private RecipeBatch ask(List<String> pantry, DietaryPreference diet, int count) throws Exception {
        LlmRequest request = prompts.build(new PromptBuilder.Inputs(pantry, List.of("salt"), diet, List.of(),
                null, null, "", count));
        return parser.parse(demo.generate(request).text());
    }

    @Test
    void producesRecipesThatPassTheSameValidationAsARealModel() throws Exception {
        RecipeBatch batch = ask(List.of("tomato", "onion", "rice"), DietaryPreference.VEG, 3);

        assertThat(batch.recipes()).hasSize(3);
        assertThat(batch.recipes()).allSatisfy(r -> {
            assertThat(r.title()).contains("(demo)");
            assertThat(r.dietaryType()).isEqualTo("VEG");
        });
        assertThat(batch.recipes().get(0).title()).contains("tomato").contains("onion");
    }

    @Test
    void honoursTheRequestedCountAndWorksWithOneIngredient() throws Exception {
        assertThat(ask(List.of("rice"), DietaryPreference.ANY, 1).recipes()).hasSize(1);
        assertThat(ask(List.of("rice"), DietaryPreference.ANY, 5).recipes()).hasSize(5);
    }

    @Test
    void labelsRecipesVeganForAVeganUser() throws Exception {
        assertThat(ask(List.of("rice", "peas"), DietaryPreference.VEGAN, 2).recipes())
                .allSatisfy(r -> assertThat(r.dietaryType()).isEqualTo("VEGAN"));
    }

    @Test
    void alwaysIncludesAPeanutRecipeSoTheAllergyFilterCanBeSeen() throws Exception {
        assertThat(ask(List.of("rice", "peas"), DietaryPreference.ANY, 3).recipes())
                .anySatisfy(r -> assertThat(r.ingredients()).extracting("name").contains("roasted peanuts"));
    }

    @Test
    void reportsItselfAsADemo() {
        assertThat(demo.modelName()).isEqualTo("demo");
        assertThat(demo.isConfigured()).isTrue();
    }
}

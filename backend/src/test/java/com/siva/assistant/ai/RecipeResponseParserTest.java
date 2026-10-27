package com.siva.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.siva.assistant.ai.RecipeDrafts.RecipeBatch;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class RecipeResponseParserTest {

    // Same Jackson settings as the running application (unknown fields are ignored)
    private final RecipeResponseParser parser = new RecipeResponseParser(
            Jackson2ObjectMapperBuilder.json().build(),
            Validation.buildDefaultValidatorFactory().getValidator());

    private static final String GOOD_RECIPE = """
            {"title":"Tomato dal","description":"Quick lentil soup","cookTimeMinutes":20,"servings":2,
             "cuisine":"Indian","dietaryType":"VEG",
             "ingredients":[{"name":"tomato","quantity":"2","unit":"pcs"},{"name":"toor dal","quantity":"1","unit":"cup"}],
             "steps":["Boil the dal","Add tomato","Temper and serve"]}
            """;

    private static String batch(String... recipes) {
        return "{\"recipes\":[" + String.join(",", recipes) + "]}";
    }

    @Test
    void readsAValidReply() throws Exception {
        RecipeBatch result = parser.parse(batch(GOOD_RECIPE));

        assertThat(result.recipes()).hasSize(1);
        var recipe = result.recipes().get(0);
        assertThat(recipe.title()).isEqualTo("Tomato dal");
        assertThat(recipe.cookTimeMinutes()).isEqualTo(20);
        assertThat(recipe.ingredients()).extracting("name").containsExactly("tomato", "toor dal");
        assertThat(recipe.steps()).hasSize(3);
    }

    @Test
    void toleratesAMarkdownFence() throws Exception {
        assertThat(parser.parse("```json\n" + batch(GOOD_RECIPE) + "\n```").recipes()).hasSize(1);
        assertThat(parser.parse("```\n" + batch(GOOD_RECIPE) + "\n```").recipes()).hasSize(1);
    }

    @Test
    void acceptsABareArrayAndNumericQuantities() throws Exception {
        String bare = "[" + GOOD_RECIPE.replace("\"quantity\":\"2\"", "\"quantity\":2") + "]";

        RecipeBatch result = parser.parse(bare);

        assertThat(result.recipes().get(0).ingredients().get(0).quantity()).isEqualTo("2");
    }

    @Test
    void ignoresFieldsItDoesNotKnow() throws Exception {
        String extra = GOOD_RECIPE.replace("\"cuisine\"", "\"calories\":300,\"cuisine\"");

        assertThat(parser.parse(batch(extra)).recipes()).hasSize(1);
    }

    @Test
    void rejectsTextThatIsNotJson() {
        assertThatThrownBy(() -> parser.parse("Sure! Here are some recipes: tomato soup"))
                .isInstanceOf(InvalidModelResponse.class).hasMessageContaining("not valid JSON");
        assertThatThrownBy(() -> parser.parse("")).isInstanceOf(InvalidModelResponse.class);
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(InvalidModelResponse.class);
    }

    @Test
    void rejectsJsonOfTheWrongShape() {
        assertThatThrownBy(() -> parser.parse("{\"recipes\":\"none\"}")).isInstanceOf(InvalidModelResponse.class);
        assertThatThrownBy(() -> parser.parse("{\"recipes\":[]}")).isInstanceOf(InvalidModelResponse.class)
                .hasMessageContaining("invalid recipe data");
        assertThatThrownBy(() -> parser.parse("42")).isInstanceOf(InvalidModelResponse.class);
    }

    @Test
    void rejectsRecipesMissingRequiredPartsAndSaysWhich() {
        String noTitle = GOOD_RECIPE.replace("\"title\":\"Tomato dal\",", "");
        String noSteps = GOOD_RECIPE.replaceAll("\"steps\":\\[[^]]*]", "\"steps\":[]");
        String noIngredients = GOOD_RECIPE.replaceAll("\"ingredients\":\\[.*?]", "\"ingredients\":[]");

        assertThatThrownBy(() -> parser.parse(batch(noTitle))).hasMessageContaining("title");
        assertThatThrownBy(() -> parser.parse(batch(noSteps))).hasMessageContaining("steps");
        assertThatThrownBy(() -> parser.parse(batch(noIngredients))).hasMessageContaining("ingredients");
    }

    @Test
    void rejectsValuesOutsideSensibleRanges() {
        String zeroMinutes = GOOD_RECIPE.replace("\"cookTimeMinutes\":20", "\"cookTimeMinutes\":0");
        String hugeServings = GOOD_RECIPE.replace("\"servings\":2", "\"servings\":500");
        String blankIngredient = GOOD_RECIPE.replace("\"name\":\"tomato\"", "\"name\":\"  \"");

        assertThatThrownBy(() -> parser.parse(batch(zeroMinutes))).hasMessageContaining("cookTimeMinutes");
        assertThatThrownBy(() -> parser.parse(batch(hugeServings))).hasMessageContaining("servings");
        assertThatThrownBy(() -> parser.parse(batch(blankIngredient))).hasMessageContaining("name");
    }

    @Test
    void rejectsAnAbsurdNumberOfRecipes() {
        String[] eleven = new String[11];
        java.util.Arrays.fill(eleven, GOOD_RECIPE);

        assertThatThrownBy(() -> parser.parse(batch(eleven))).hasMessageContaining("recipes");
    }
}

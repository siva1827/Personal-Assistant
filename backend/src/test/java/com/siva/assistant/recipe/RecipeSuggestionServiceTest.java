package com.siva.assistant.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.ai.AiProperties;
import com.siva.assistant.ai.LlmClient;
import com.siva.assistant.ai.LlmException;
import com.siva.assistant.ai.LlmReply;
import com.siva.assistant.ai.LlmRequest;
import com.siva.assistant.ai.PromptBuilder;
import com.siva.assistant.ai.RecipeResponseParser;
import com.siva.assistant.common.ApiException;
import com.siva.assistant.ingredient.Ingredient;
import com.siva.assistant.ingredient.IngredientRepository;
import com.siva.assistant.pantry.PantryItem;
import com.siva.assistant.pantry.PantryItemRepository;
import com.siva.assistant.recipe.SuggestionDtos.SuggestRequest;
import com.siva.assistant.recipe.SuggestionDtos.SuggestionResponse;
import com.siva.assistant.theme.DayTheme;
import com.siva.assistant.theme.DayThemeRepository;
import com.siva.assistant.user.DietaryPreference;
import com.siva.assistant.user.User;
import com.siva.assistant.user.UserRepository;
import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RecipeSuggestionServiceTest {

    private static final long USER = 7L;
    private static final Instant NOW = Instant.parse("2026-10-27T06:00:00Z");

    @Mock
    LlmClient llm;
    @Mock
    UserRepository users;
    @Mock
    PantryItemRepository pantry;
    @Mock
    IngredientRepository ingredients;
    @Mock
    DayThemeRepository themes;
    @Mock
    RecipeRequestRepository requests;
    @Mock
    RecipeRepository recipes;
    @Mock
    RecipeService recipeService;

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private RecipeSuggestionService service;
    private User user;

    /** Runs the callbacks directly; there is no database in these tests. */
    private static final class NoTransactions implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }

    @BeforeEach
    void setUp() {
        service = build(new AiProperties("auto", 3, 30, null));
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER);
        lenient().when(llm.isConfigured()).thenReturn(true);
        lenient().when(llm.modelName()).thenReturn("test-model");
        lenient().when(users.findById(USER)).thenReturn(Optional.of(user));
        lenient().when(pantry.findByUserIdOrderByAddedAtDescIdDesc(USER))
                .thenReturn(List.of(item("tomato", false), item("onion", false), item("salt", true)));
        lenient().when(ingredients.findByStapleTrue()).thenReturn(List.of(ingredient("salt", true),
                ingredient("water", true)));
        lenient().when(requests.countByUserIdAndCreatedAtGreaterThanEqual(anyLong(), any())).thenReturn(0L);
        lenient().when(requests.findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
                anyLong(), any(), any(), any())).thenReturn(Optional.empty());
        lenient().when(requests.save(any())).thenAnswer(inv -> {
            RecipeRequest r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 100L);
            return r;
        });
        lenient().when(recipes.saveAll(any())).thenAnswer(inv -> {
            List<Recipe> saved = new ArrayList<>(inv.<List<Recipe>>getArgument(0));
            long id = 1;
            for (Recipe r : saved) {
                ReflectionTestUtils.setField(r, "id", id++);
            }
            return saved;
        });
        lenient().when(recipeService.responsesFor(anyLong(), any())).thenAnswer(inv ->
                inv.<List<Recipe>>getArgument(1).stream().map(this::responseFor).toList());
    }

    private RecipeSuggestionService build(AiProperties props) {
        return new RecipeSuggestionService(llm, props, new PromptBuilder(),
                new RecipeResponseParser(mapper, Validation.buildDefaultValidatorFactory().getValidator()),
                users, pantry, ingredients, themes, requests, recipes, recipeService, mapper,
                new TransactionTemplate(new NoTransactions()), Clock.fixed(NOW, ZoneId.of("UTC")));
    }

    private static Ingredient ingredient(String name, boolean staple) {
        Ingredient i = new Ingredient();
        ReflectionTestUtils.setField(i, "name", name);
        ReflectionTestUtils.setField(i, "staple", staple);
        return i;
    }

    private static PantryItem item(String name, boolean staple) {
        return new PantryItem(USER, ingredient(name, staple));
    }

    private com.siva.assistant.recipe.RecipeResponse responseFor(Recipe r) {
        try {
            JsonNode ingredientsJson = mapper.readTree(r.getIngredients());
            JsonNode missing = mapper.readTree(r.getMissingIngredients());
            JsonNode steps = mapper.readTree(r.getSteps());
            return new RecipeResponse(r.getId(), r.getTitle(), r.getDescription(), r.getCookTimeMinutes(),
                    r.getServings(), r.getCuisine(), r.getDietaryType(), ingredientsJson, missing, steps, false, null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String recipeJson(String title, String diet, String... ingredientNames) {
        StringBuilder ing = new StringBuilder();
        for (String name : ingredientNames) {
            ing.append(ing.isEmpty() ? "" : ",").append("{\"name\":\"").append(name)
                    .append("\",\"quantity\":\"1\",\"unit\":\"cup\"}");
        }
        return "{\"title\":\"" + title + "\",\"description\":\"d\",\"cookTimeMinutes\":20,\"servings\":2,"
                + "\"cuisine\":\"Indian\",\"dietaryType\":\"" + diet + "\",\"ingredients\":[" + ing
                + "],\"steps\":[\"Chop\",\"Cook\"]}";
    }

    private static String batch(String... recipes) {
        return "{\"recipes\":[" + String.join(",", recipes) + "]}";
    }

    private static final SuggestRequest REQUEST = new SuggestRequest(null, null, null, null);

    // ---- gatekeeping ----

    @Test
    void answers503WithoutTouchingAnythingWhenNoProviderIsConfigured() {
        when(llm.isConfigured()).thenReturn(false);

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verify(users, never()).findById(any());
        verify(llm, never()).generate(any());
    }

    @Test
    void asksTheUserToAddIngredientsWhenOnlyStaplesAreInThePantry() {
        when(pantry.findByUserIdOrderByAddedAtDescIdDesc(USER)).thenReturn(List.of(item("salt", true)));

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(e.getMessage()).contains("pantry");
        });
        verify(llm, never()).generate(any());
    }

    @Test
    void answers404ForAnUnknownTheme() {
        when(themes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggest(USER, new SuggestRequest(99L, null, null, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void stopsAtTheDailyLimitBeforeSpendingMoney() {
        when(requests.countByUserIdAndCreatedAtGreaterThanEqual(anyLong(), any())).thenReturn(3L);

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(e.getMessage()).contains("3");
        });
        verify(llm, never()).generate(any());
    }

    @Test
    void countsTheDayFromMidnightInTheServersTimezone() {
        when(llm.generate(any())).thenReturn(new LlmReply(batch(recipeJson("A", "VEG", "tomato")), 10));

        service.suggest(USER, REQUEST);

        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(requests).countByUserIdAndCreatedAtGreaterThanEqual(org.mockito.ArgumentMatchers.eq(USER), since.capture());
        assertThat(since.getValue()).isEqualTo(Instant.parse("2026-10-27T00:00:00Z"));
    }

    // ---- the happy path ----

    @Test
    void savesValidatedRecipesAndComparesIngredientsWithTheRealPantry() {
        when(llm.generate(any())).thenReturn(new LlmReply(
                batch(recipeJson("Tomato curry", "VEG", "2 ripe tomatoes", "onion", "salt", "cumin seeds")), 120));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.cached()).isFalse();
        assertThat(response.filteredOut()).isZero();
        assertThat(response.model()).isEqualTo("test-model");
        assertThat(response.recipes()).hasSize(1);

        ArgumentCaptor<List<Recipe>> saved = ArgumentCaptor.forClass(List.class);
        verify(recipes).saveAll(saved.capture());
        Recipe recipe = saved.getValue().get(0);
        assertThat(recipe.getTitle()).isEqualTo("Tomato curry");
        assertThat(recipe.getRequestId()).isEqualTo(100L);
        assertThat(recipe.getUserId()).isEqualTo(USER);
        assertThat(recipe.getDietaryType()).isEqualTo(DietaryPreference.VEG);
        // the pantry has tomato and onion, salt is a staple: only cumin has to be bought
        assertThat(recipe.getMissingIngredients()).isEqualTo("[\"cumin seeds\"]");
        assertThat(recipe.getIngredients()).contains("\"in_pantry\":true").contains("\"in_pantry\":false");
        assertThat(recipe.getSteps()).isEqualTo("[\"Chop\",\"Cook\"]");

        ArgumentCaptor<RecipeRequest> request = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(requests).save(request.capture());
        assertThat(request.getValue().getStatus()).isEqualTo(RequestStatus.SUCCESS);
        assertThat(request.getValue().getTokensUsed()).isEqualTo(120);
        assertThat(request.getValue().getRequestHash()).hasSize(64);
    }

    @Test
    void ignoresWhatTheModelClaimsAboutOwnershipAndDecidesItself() {
        String liar = "{\"title\":\"X\",\"cookTimeMinutes\":10,\"servings\":1,\"dietaryType\":\"VEG\","
                + "\"ingredients\":[{\"name\":\"saffron\",\"in_pantry\":true,\"inPantry\":true}],\"steps\":[\"Cook\"]}";
        when(llm.generate(any())).thenReturn(new LlmReply(batch(liar), 5));

        service.suggest(USER, REQUEST);

        ArgumentCaptor<List<Recipe>> saved = ArgumentCaptor.forClass(List.class);
        verify(recipes).saveAll(saved.capture());
        assertThat(saved.getValue().get(0).getMissingIngredients()).isEqualTo("[\"saffron\"]");
    }

    // ---- safety filters ----

    @Test
    void dropsRecipesThatContainAnAllergenEvenIfItIsOnlyInTheSteps() {
        user.setAllergies(List.of("peanut", "shellfish"));
        String sneaky = "{\"title\":\"Salad\",\"cookTimeMinutes\":10,\"servings\":1,\"dietaryType\":\"VEG\","
                + "\"ingredients\":[{\"name\":\"tomato\"}],\"steps\":[\"Chop\",\"Garnish with crushed peanuts\"]}";
        when(llm.generate(any())).thenReturn(new LlmReply(batch(
                recipeJson("Peanut bowl", "VEG", "tomato", "roasted peanuts"),
                recipeJson("Prawn curry", "NON_VEG", "tomato", "prawns"),
                sneaky,
                recipeJson("Safe curry", "VEG", "tomato", "onion")), 50));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.recipes()).extracting("title").containsExactly("Safe curry");
        assertThat(response.filteredOut()).isEqualTo(3);
    }

    @Test
    void dropsRecipesThatDoNotFitTheDiet() {
        user.setDietaryPreference(DietaryPreference.VEGAN);
        when(llm.generate(any())).thenReturn(new LlmReply(batch(
                recipeJson("Vegan dal", "VEGAN", "tomato"),
                recipeJson("Paneer curry", "VEG", "tomato"),
                recipeJson("Chicken curry", "NON_VEG", "tomato"),
                recipeJson("Mystery", "MAYBE", "tomato")), 50));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.recipes()).extracting("title").containsExactly("Vegan dal");
        assertThat(response.filteredOut()).isEqualTo(3);
    }

    @Test
    void aVegetarianMayReceiveVeganRecipesButNotEggOrMeat() {
        user.setDietaryPreference(DietaryPreference.VEG);
        when(llm.generate(any())).thenReturn(new LlmReply(batch(
                recipeJson("A", "veg", "tomato"), recipeJson("B", "vegan", "tomato"),
                recipeJson("C", "EGGETARIAN", "tomato"), recipeJson("D", "non-veg", "tomato")), 1));

        assertThat(service.suggest(USER, REQUEST).recipes()).extracting("title").containsExactly("A", "B");
    }

    @Test
    void someoneWithoutARestrictionGetsEverythingAndTypesAreReadLeniently() {
        when(llm.generate(any())).thenReturn(new LlmReply(batch(
                recipeJson("A", "Vegetarian", "tomato"), recipeJson("B", "weird", "tomato")), 1));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.recipes()).hasSize(2);
        assertThat(response.recipes().get(0).dietaryType()).isEqualTo(DietaryPreference.VEG);
        assertThat(response.recipes().get(1).dietaryType()).isEqualTo(DietaryPreference.ANY);
    }

    @Test
    void reportsAnEmptyResultWhenEverythingWasFilteredOut() {
        user.setAllergies(List.of("tomato"));
        when(llm.generate(any())).thenReturn(new LlmReply(batch(recipeJson("A", "VEG", "tomato")), 1));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.recipes()).isEmpty();
        assertThat(response.filteredOut()).isEqualTo(1);
    }

    // ---- bad model output ----

    @Test
    void asksOnceMoreWithARepairInstructionAndAddsUpTheTokens() {
        when(llm.generate(any()))
                .thenReturn(new LlmReply("Sure! Here you go: tomato soup", 40))
                .thenReturn(new LlmReply(batch(recipeJson("Fixed", "VEG", "tomato")), 60));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.recipes()).extracting("title").containsExactly("Fixed");
        ArgumentCaptor<LlmRequest> sent = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm, times(2)).generate(sent.capture());
        LlmRequest repair = sent.getAllValues().get(1);
        assertThat(repair.turns()).hasSize(3);
        assertThat(repair.turns().get(1).role()).isEqualTo(LlmRequest.Role.MODEL);
        assertThat(repair.turns().get(1).text()).isEqualTo("Sure! Here you go: tomato soup");
        assertThat(repair.turns().get(2).text()).contains("could not be used").contains("not valid JSON");

        ArgumentCaptor<RecipeRequest> request = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(requests).save(request.capture());
        assertThat(request.getValue().getTokensUsed()).isEqualTo(100);
        assertThat(request.getValue().getStatus()).isEqualTo(RequestStatus.SUCCESS);
    }

    @Test
    void givesUpAfterTheSecondBadReplyAndRecordsTheFailure() {
        when(llm.generate(any())).thenReturn(new LlmReply("nope", 30), new LlmReply("still nope", 30));

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(e.getMessage()).contains("could not be used");
        });

        verify(llm, times(2)).generate(any());
        verify(recipes, never()).saveAll(any());
        ArgumentCaptor<RecipeRequest> request = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(requests).save(request.capture());
        assertThat(request.getValue().getStatus()).isEqualTo(RequestStatus.INVALID_RESPONSE);
        assertThat(request.getValue().getTokensUsed()).isEqualTo(60);
    }

    @Test
    void treatsAReplyThatBreaksTheRulesAsInvalidToo() {
        String hugeServings = recipeJson("A", "VEG", "tomato").replace("\"servings\":2", "\"servings\":900");
        when(llm.generate(any())).thenReturn(new LlmReply(batch(hugeServings), 1), new LlmReply(batch(hugeServings), 1));

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void reportsProviderFailuresAs502AndRecordsThemAsFailed() {
        when(llm.generate(any())).thenThrow(new LlmException("The AI service quota or rate limit was reached. Try again later"));

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(e.getMessage()).contains("quota");
        });

        verify(llm, times(1)).generate(any());
        ArgumentCaptor<RecipeRequest> request = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(requests).save(request.capture());
        assertThat(request.getValue().getStatus()).isEqualTo(RequestStatus.FAILED);
    }

    @Test
    void aProviderFailureDuringTheRepairAttemptKeepsTheTokensAlreadySpent() {
        when(llm.generate(any())).thenReturn(new LlmReply("garbage", 25)).thenThrow(new LlmException("down"));

        assertThatThrownBy(() -> service.suggest(USER, REQUEST)).isInstanceOf(ApiException.class);

        ArgumentCaptor<RecipeRequest> request = ArgumentCaptor.forClass(RecipeRequest.class);
        verify(requests).save(request.capture());
        assertThat(request.getValue().getStatus()).isEqualTo(RequestStatus.FAILED);
        assertThat(request.getValue().getTokensUsed()).isEqualTo(25);
    }

    // ---- cost control ----

    @Test
    void reusesRecentSuggestionsForTheSameInputsWithoutCallingTheModelOrCountingTowardsTheLimit() {
        RecipeRequest recent = new RecipeRequest(USER, null, null, "[]", "m", 10, RequestStatus.SUCCESS, "h");
        ReflectionTestUtils.setField(recent, "id", 55L);
        when(requests.findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
                anyLong(), any(), any(), any())).thenReturn(Optional.of(recent));
        Recipe old = new Recipe(USER, 55L, "Earlier", null, 10, 2, null, DietaryPreference.VEG,
                "[{\"name\":\"tomato\",\"in_pantry\":true}]", "[]", "[\"Cook\"]");
        ReflectionTestUtils.setField(old, "id", 9L);
        when(recipes.findByRequestIdOrderByIdAsc(55L)).thenReturn(List.of(old));

        SuggestionResponse response = service.suggest(USER, REQUEST);

        assertThat(response.cached()).isTrue();
        assertThat(response.recipes()).extracting("title").containsExactly("Earlier");
        verify(llm, never()).generate(any());
        verify(requests, never()).save(any());
        verify(requests, never()).countByUserIdAndCreatedAtGreaterThanEqual(anyLong(), any());
    }

    @Test
    void askingForFreshIdeasSkipsTheCache() {
        when(llm.generate(any())).thenReturn(new LlmReply(batch(recipeJson("New", "VEG", "tomato")), 5));

        SuggestionResponse response = service.suggest(USER, new SuggestRequest(null, null, null, true));

        assertThat(response.cached()).isFalse();
        verify(requests, never())
                .findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
                        anyLong(), any(), any(), any());
    }

    @Test
    void theCacheCanBeSwitchedOff() {
        service = build(new AiProperties("auto", 3, 0, null));
        when(llm.generate(any())).thenReturn(new LlmReply(batch(recipeJson("New", "VEG", "tomato")), 5));

        service.suggest(USER, REQUEST);

        verify(requests, never())
                .findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
                        anyLong(), any(), any(), any());
    }

    @Test
    void theFingerprintIgnoresOrderAndCaseButNotRealChanges() {
        String base = RecipeSuggestionService.fingerprint(List.of("onion", "tomato"), 1L, "light", 3,
                DietaryPreference.VEG, List.of("Peanut", "egg"));

        assertThat(RecipeSuggestionService.fingerprint(List.of("tomato", "onion"), 1L, "light", 3,
                DietaryPreference.VEG, List.of("EGG", "peanut"))).isEqualTo(base);
        assertThat(RecipeSuggestionService.fingerprint(List.of("tomato", "onion", "rice"), 1L, "light", 3,
                DietaryPreference.VEG, List.of("peanut", "egg"))).isNotEqualTo(base);
        assertThat(RecipeSuggestionService.fingerprint(List.of("tomato", "onion"), 2L, "light", 3,
                DietaryPreference.VEG, List.of("peanut", "egg"))).isNotEqualTo(base);
        assertThat(RecipeSuggestionService.fingerprint(List.of("tomato", "onion"), 1L, "light", 4,
                DietaryPreference.VEG, List.of("peanut", "egg"))).isNotEqualTo(base);
        assertThat(RecipeSuggestionService.fingerprint(List.of("tomato", "onion"), 1L, "light", 3,
                DietaryPreference.VEGAN, List.of("peanut", "egg"))).isNotEqualTo(base);
    }

    // ---- what is sent to the model ----

    @Test
    void sendsThePantryThemeNoteAndRestrictionsToTheModel() {
        user.setAllergies(List.of("peanut"));
        user.setDietaryPreference(DietaryPreference.VEG);
        DayTheme theme = new DayTheme();
        ReflectionTestUtils.setField(theme, "id", 1L);
        ReflectionTestUtils.setField(theme, "label", "Quick (15 min)");
        ReflectionTestUtils.setField(theme, "promptHint", "Under 15 minutes.");
        when(themes.findById(1L)).thenReturn(Optional.of(theme));
        when(llm.generate(any())).thenReturn(new LlmReply(batch(recipeJson("A", "VEG", "tomato")), 1));

        service.suggest(USER, new SuggestRequest(1L, "something light, I am tired", 2, null));

        ArgumentCaptor<LlmRequest> sent = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llm).generate(sent.capture());
        String text = sent.getValue().turns().get(0).text();
        assertThat(text).contains("Pantry: tomato, onion").doesNotContain("Pantry: tomato, onion, salt")
                .contains("Always available: salt, water").contains("Diet: VEG")
                .contains("Allergies (never use): peanut").contains("Quick (15 min)").contains("Under 15 minutes")
                .contains("something light, I am tired").contains("Number of recipes: 2");
    }
}

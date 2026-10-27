package com.siva.assistant.recipe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siva.assistant.ai.AiProperties;
import com.siva.assistant.ai.IngredientMatcher;
import com.siva.assistant.ai.InvalidModelResponse;
import com.siva.assistant.ai.LlmClient;
import com.siva.assistant.ai.LlmException;
import com.siva.assistant.ai.LlmReply;
import com.siva.assistant.ai.LlmRequest;
import com.siva.assistant.ai.PromptBuilder;
import com.siva.assistant.ai.RecipeDrafts.IngredientDraft;
import com.siva.assistant.ai.RecipeDrafts.RecipeBatch;
import com.siva.assistant.ai.RecipeDrafts.RecipeDraft;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Suggests recipes from the user's pantry with an AI model, and does not trust the answer.
 *
 * The slow model call happens outside any database transaction. Before it: the daily limit, the
 * short-term cache and the input checks. After it: the reply is validated (one repair retry),
 * recipes that clash with allergies or diet are dropped in code, ingredients are compared with
 * the real pantry, and every attempt is recorded with its token cost.
 */
@Service
public class RecipeSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(RecipeSuggestionService.class);
    private static final int DEFAULT_COUNT = 3;

    private static final Set<DietaryPreference> ANYTHING = Set.of(DietaryPreference.values());
    /** Which declared recipe types each diet may receive. */
    private static final Map<DietaryPreference, Set<DietaryPreference>> ALLOWED = Map.of(
            DietaryPreference.ANY, ANYTHING,
            DietaryPreference.NON_VEG, ANYTHING,
            DietaryPreference.EGGETARIAN,
            Set.of(DietaryPreference.VEG, DietaryPreference.EGGETARIAN, DietaryPreference.VEGAN),
            DietaryPreference.VEG, Set.of(DietaryPreference.VEG, DietaryPreference.VEGAN),
            DietaryPreference.VEGAN, Set.of(DietaryPreference.VEGAN));

    private final LlmClient llm;
    private final AiProperties properties;
    private final PromptBuilder prompts;
    private final RecipeResponseParser parser;
    private final UserRepository users;
    private final PantryItemRepository pantry;
    private final IngredientRepository ingredients;
    private final DayThemeRepository themes;
    private final RecipeRequestRepository requests;
    private final RecipeRepository recipes;
    private final RecipeService recipeService;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RecipeSuggestionService(LlmClient llm, AiProperties properties, PromptBuilder prompts,
                                   RecipeResponseParser parser, UserRepository users, PantryItemRepository pantry,
                                   IngredientRepository ingredients, DayThemeRepository themes,
                                   RecipeRequestRepository requests, RecipeRepository recipes,
                                   RecipeService recipeService, ObjectMapper mapper, TransactionTemplate tx,
                                   Clock clock) {
        this.llm = llm;
        this.properties = properties;
        this.prompts = prompts;
        this.parser = parser;
        this.users = users;
        this.pantry = pantry;
        this.ingredients = ingredients;
        this.themes = themes;
        this.requests = requests;
        this.recipes = recipes;
        this.recipeService = recipeService;
        this.mapper = mapper;
        this.tx = tx;
        this.clock = clock;
    }

    /** Everything gathered before the model is called. cached is set when no call is needed. */
    private record Prepared(
            SuggestionResponse cached,
            long userId,
            Long themeId,
            String note,
            int count,
            DietaryPreference diet,
            List<String> allergies,
            List<String> pantryNames,
            List<String> staples,
            String themeLabel,
            String themeHint,
            String hash) {
    }

    private record Generation(RecipeBatch batch, int tokens) {
    }

    private static final class GenerationFailed extends Exception {
        private final RequestStatus status;
        private final int tokens;

        GenerationFailed(RequestStatus status, int tokens, String message) {
            super(message);
            this.status = status;
            this.tokens = tokens;
        }
    }

    public SuggestionResponse suggest(long userId, SuggestRequest request) {
        if (!llm.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI suggestions are not set up yet. Add an API key on the server (see docs/AI_SETUP.md)");
        }
        int count = request.count() == null ? DEFAULT_COUNT : request.count();
        Prepared prepared = tx.execute(status -> prepare(userId, request, count));
        if (prepared == null) {
            throw new IllegalStateException("Could not prepare the request");
        }
        if (prepared.cached() != null) {
            return prepared.cached();
        }

        Generation generation;
        try {
            generation = generate(prepared);
        } catch (GenerationFailed failure) {
            tx.executeWithoutResult(status -> requests.save(record(prepared, failure.tokens, failure.status)));
            throw new ApiException(HttpStatus.BAD_GATEWAY, failure.getMessage());
        }
        SuggestionResponse response = tx.execute(status -> save(prepared, generation));
        if (response == null) {
            throw new IllegalStateException("Could not save the suggestions");
        }
        return response;
    }

    private Prepared prepare(long userId, SuggestRequest request, int count) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        List<String> pantryNames = pantry.findByUserIdOrderByAddedAtDescIdDesc(userId).stream()
                .map(PantryItem::getIngredient)
                .filter(i -> !i.isStaple())
                .map(Ingredient::getName)
                .distinct()
                .toList();
        if (pantryNames.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add some ingredients to your pantry first");
        }
        List<String> staples = ingredients.findByStapleTrue().stream().map(Ingredient::getName).toList();

        DayTheme theme = request.themeId() == null ? null : themes.findById(request.themeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Theme not found"));
        String note = PromptBuilder.clean(request.customNote(), PromptBuilder.MAX_NOTE_LENGTH);
        List<String> allergies = user.getAllergies() == null ? List.of() : List.copyOf(user.getAllergies());
        DietaryPreference diet = user.getDietaryPreference() == null ? DietaryPreference.ANY : user.getDietaryPreference();

        String hash = fingerprint(pantryNames, theme == null ? null : theme.getId(), note, count, diet, allergies);

        boolean fresh = Boolean.TRUE.equals(request.fresh());
        if (!fresh && properties.cacheMinutes() > 0) {
            Instant since = clock.instant().minus(Duration.ofMinutes(properties.cacheMinutes()));
            Optional<RecipeRequest> recent = requests
                    .findFirstByUserIdAndRequestHashAndStatusAndCreatedAtGreaterThanEqualOrderByIdDesc(
                            userId, hash, RequestStatus.SUCCESS, since);
            if (recent.isPresent()) {
                List<Recipe> saved = recipes.findByRequestIdOrderByIdAsc(recent.get().getId());
                return cachedResult(userId, saved);
            }
        }

        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        if (requests.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfDay) >= properties.dailyLimit()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have used all " + properties.dailyLimit() + " AI suggestions for today. Try again tomorrow");
        }

        return new Prepared(null, userId, theme == null ? null : theme.getId(), note, count, diet, allergies,
                pantryNames, staples, theme == null ? null : theme.getLabel(),
                theme == null ? null : theme.getPromptHint(), hash);
    }

    private Prepared cachedResult(long userId, List<Recipe> saved) {
        SuggestionResponse response = new SuggestionResponse(recipeService.responsesFor(userId, saved), true, 0,
                llm.modelName());
        return new Prepared(response, userId, null, "", 0, DietaryPreference.ANY, List.of(), List.of(), List.of(),
                null, null, "");
    }

    /** Calls the model, and once more with a repair instruction if the first answer is unusable. */
    private Generation generate(Prepared p) throws GenerationFailed {
        LlmRequest first = prompts.build(new PromptBuilder.Inputs(p.pantryNames(), p.staples(), p.diet(),
                p.allergies(), p.themeLabel(), p.themeHint(), p.note(), p.count()));

        LlmReply firstReply = call(first, 0);
        int tokens = tokensOf(firstReply);
        try {
            return new Generation(parser.parse(firstReply.text()), tokens);
        } catch (InvalidModelResponse firstProblem) {
            log.info("The first AI reply was unusable ({}); asking once more", firstProblem.getMessage());
            LlmReply secondReply = call(
                    first.withFollowUp(firstReply.text(), prompts.repairInstruction(firstProblem.getMessage())), tokens);
            tokens += tokensOf(secondReply);
            try {
                return new Generation(parser.parse(secondReply.text()), tokens);
            } catch (InvalidModelResponse secondProblem) {
                log.warn("The AI reply was unusable twice: {}", secondProblem.getMessage());
                throw new GenerationFailed(RequestStatus.INVALID_RESPONSE, tokens,
                        "The AI gave an answer that could not be used. Please try again");
            }
        }
    }

    private LlmReply call(LlmRequest request, int tokensSoFar) throws GenerationFailed {
        try {
            return llm.generate(request);
        } catch (LlmException e) {
            log.warn("AI call failed: {}", e.getMessage());
            throw new GenerationFailed(RequestStatus.FAILED, tokensSoFar, e.getMessage());
        }
    }

    private SuggestionResponse save(Prepared p, Generation generation) {
        List<String> knownIngredients = new ArrayList<>(p.pantryNames());
        knownIngredients.addAll(p.staples());

        List<RecipeDraft> safe = new ArrayList<>();
        int filteredOut = 0;
        for (RecipeDraft draft : generation.batch().recipes()) {
            if (conflictsWithDiet(p.diet(), draft) || mentionsAllergen(draft, p.allergies())) {
                filteredOut++;
            } else {
                safe.add(draft);
            }
        }

        RecipeRequest request = requests.save(record(p, generation.tokens(), RequestStatus.SUCCESS));
        List<Recipe> toSave = new ArrayList<>();
        for (RecipeDraft draft : safe) {
            toSave.add(toRecipe(p.userId(), request.getId(), draft, knownIngredients));
        }
        List<Recipe> saved = recipes.saveAll(toSave);
        return new SuggestionResponse(recipeService.responsesFor(p.userId(), saved), false, filteredOut,
                llm.modelName());
    }

    private RecipeRequest record(Prepared p, int tokens, RequestStatus status) {
        return new RecipeRequest(p.userId(), p.themeId(), p.note().isEmpty() ? null : p.note(),
                json(p.pantryNames()), llm.modelName(), tokens, status, p.hash());
    }

    private Recipe toRecipe(long userId, long requestId, RecipeDraft draft, List<String> knownIngredients) {
        List<Map<String, Object>> stored = new ArrayList<>();
        Set<String> missing = new LinkedHashSet<>();
        for (IngredientDraft ingredient : draft.ingredients()) {
            // Whether the user has it is decided here, against the real pantry, not by the model
            boolean have = IngredientMatcher.inPantry(ingredient.name(), knownIngredients);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", ingredient.name().trim());
            row.put("quantity", blankToNull(ingredient.quantity()));
            row.put("unit", blankToNull(ingredient.unit()));
            row.put("in_pantry", have);
            stored.add(row);
            if (!have) {
                missing.add(ingredient.name().trim().toLowerCase(Locale.ROOT));
            }
        }
        return new Recipe(userId, requestId, draft.title().trim(), blankToNull(draft.description()),
                draft.cookTimeMinutes(), draft.servings(), blankToNull(draft.cuisine()),
                declaredDiet(draft).orElse(DietaryPreference.ANY), json(stored), json(List.copyOf(missing)),
                json(draft.steps().stream().map(String::trim).toList()));
    }

    // ---- Safety checks, run in code because the model's own claims cannot be trusted ----

    static boolean mentionsAllergen(RecipeDraft draft, List<String> allergies) {
        if (allergies.isEmpty()) {
            return false;
        }
        StringBuilder text = new StringBuilder(draft.title()).append(' ');
        if (draft.description() != null) {
            text.append(draft.description()).append(' ');
        }
        draft.ingredients().forEach(i -> text.append(i.name()).append(' '));
        draft.steps().forEach(s -> text.append(s).append(' '));
        return IngredientMatcher.mentionsAllergen(text.toString(), allergies);
    }

    /** A recipe whose declared type is not allowed for the diet, or cannot be read, is dropped. */
    static boolean conflictsWithDiet(DietaryPreference diet, RecipeDraft draft) {
        if (ALLOWED.get(diet) == ANYTHING) {
            return false;
        }
        return declaredDiet(draft).map(d -> !ALLOWED.get(diet).contains(d)).orElse(true);
    }

    static Optional<DietaryPreference> declaredDiet(RecipeDraft draft) {
        if (draft.dietaryType() == null) {
            return Optional.empty();
        }
        String normalized = draft.dietaryType().trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "VEG", "VEGETARIAN" -> Optional.of(DietaryPreference.VEG);
            case "NON_VEG", "NONVEG", "NON_VEGETARIAN" -> Optional.of(DietaryPreference.NON_VEG);
            case "VEGAN" -> Optional.of(DietaryPreference.VEGAN);
            case "EGGETARIAN", "EGG" -> Optional.of(DietaryPreference.EGGETARIAN);
            default -> Optional.empty();
        };
    }

    // ---- Small helpers ----

    private static int tokensOf(LlmReply reply) {
        return reply.totalTokens() == null ? 0 : reply.totalTokens();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not store recipe data", e);
        }
    }

    /** SHA-256 over everything that shapes the answer; the same inputs give the same value. */
    static String fingerprint(List<String> pantryNames, Long themeId, String note, int count,
                              DietaryPreference diet, List<String> allergies) {
        List<String> pantrySorted = new ArrayList<>(pantryNames);
        Collections.sort(pantrySorted);
        List<String> allergiesSorted = new ArrayList<>(allergies.stream().map(a -> a.toLowerCase(Locale.ROOT)).toList());
        Collections.sort(allergiesSorted);
        String text = String.join("|", String.join(",", pantrySorted), String.valueOf(themeId), note,
                String.valueOf(count), diet.name(), String.join(",", allergiesSorted));
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

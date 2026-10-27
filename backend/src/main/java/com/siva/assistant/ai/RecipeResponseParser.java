package com.siva.assistant.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.siva.assistant.ai.RecipeDrafts.RecipeBatch;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Turns the model's text into validated recipes, or explains precisely what was wrong. */
@Component
public class RecipeResponseParser {

    private final ObjectMapper mapper;
    private final Validator validator;

    public RecipeResponseParser(ObjectMapper mapper, Validator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public RecipeBatch parse(String text) throws InvalidModelResponse {
        JsonNode root;
        try {
            root = mapper.readTree(stripFences(text));
        } catch (JsonProcessingException e) {
            throw new InvalidModelResponse("the reply was not valid JSON", e);
        }
        if (root == null || root.isMissingNode() || root.isNull()) {
            throw new InvalidModelResponse("the reply was empty");
        }
        // Some models return the bare array instead of {"recipes": [...]}
        if (root.isArray()) {
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.set("recipes", root);
            root = wrapper;
        }

        RecipeBatch batch;
        try {
            batch = mapper.treeToValue(root, RecipeBatch.class);
        } catch (JsonProcessingException e) {
            throw new InvalidModelResponse("the JSON did not match the schema", e);
        }
        Set<ConstraintViolation<RecipeBatch>> problems = validator.validate(batch);
        if (!problems.isEmpty()) {
            String detail = problems.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .sorted()
                    .limit(5)
                    .collect(Collectors.joining("; "));
            throw new InvalidModelResponse("invalid recipe data: " + detail);
        }
        return batch;
    }

    /** Models sometimes wrap JSON in a markdown fence even when told not to. */
    static String stripFences(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            trimmed = firstNewline < 0 ? "" : trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }
}

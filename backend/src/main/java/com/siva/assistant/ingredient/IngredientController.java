package com.siva.assistant.ingredient;

import com.siva.assistant.common.ApiException;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingredients")
public class IngredientController {

    private static final int MAX_RESULTS = 20;

    private final IngredientRepository ingredients;

    public IngredientController(IngredientRepository ingredients) {
        this.ingredients = ingredients;
    }

    /** Autocomplete: case-insensitive substring match on the ingredient name. */
    @GetMapping
    public List<IngredientResponse> search(@RequestParam(defaultValue = "") String query) {
        return ingredients.findByNameContainingIgnoreCaseOrderByName(query.trim(), PageRequest.of(0, MAX_RESULTS))
                .stream().map(IngredientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public IngredientResponse get(@PathVariable Long id) {
        return ingredients.findById(id).map(IngredientResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ingredient not found"));
    }
}

package com.siva.assistant.pantry;

import com.siva.assistant.pantry.PantryDtos.AddItemRequest;
import com.siva.assistant.pantry.PantryDtos.BulkAddRequest;
import com.siva.assistant.pantry.PantryDtos.ItemResponse;
import com.siva.assistant.pantry.PantryDtos.UpdateItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pantry")
@Validated
public class PantryController {

    private final PantryService pantryService;

    public PantryController(PantryService pantryService) {
        this.pantryService = pantryService;
    }

    @GetMapping
    public List<ItemResponse> list(@AuthenticationPrincipal Long userId) {
        return pantryService.list(userId);
    }

    @GetMapping("/expiring")
    public List<ItemResponse> expiring(@AuthenticationPrincipal Long userId,
                                       @RequestParam(defaultValue = "3") @Min(0) @Max(365) int days) {
        return pantryService.expiring(userId, days);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse add(@AuthenticationPrincipal Long userId, @Valid @RequestBody AddItemRequest request) {
        return pantryService.add(userId, request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ItemResponse> addBulk(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody BulkAddRequest request) {
        return pantryService.addBulk(userId, request);
    }

    @PatchMapping("/{id}")
    public ItemResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                               @Valid @RequestBody UpdateItemRequest request) {
        return pantryService.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        pantryService.delete(userId, id);
    }
}

package com.siva.assistant.theme;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/themes")
public class ThemeController {

    /** The prompt hint is internal and not exposed to clients. */
    public record ThemeResponse(Long id, String code, String label) {
    }

    private final DayThemeRepository themes;

    public ThemeController(DayThemeRepository themes) {
        this.themes = themes;
    }

    @GetMapping
    public List<ThemeResponse> list() {
        return themes.findAll(Sort.by("id")).stream()
                .map(t -> new ThemeResponse(t.getId(), t.getCode(), t.getLabel()))
                .toList();
    }
}

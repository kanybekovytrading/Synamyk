package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.NewsDetailResponse;
import synamyk.dto.admin.CreateNewsRequest;
import synamyk.service.NewsService;

@RestController
@RequestMapping("/api/admin/news")
@RequiredArgsConstructor
@Tag(name = "Admin - News", description = "Admin: create and manage news articles")
public class AdminFeedController {

    private final NewsService newsService;

    @PostMapping
    @Operation(summary = "Create news article")
    public ResponseEntity<NewsDetailResponse> create(@Valid @RequestBody CreateNewsRequest request) {
        return ResponseEntity.ok(newsService.createNews(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update news article")
    public ResponseEntity<NewsDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateNewsRequest request) {
        return ResponseEntity.ok(newsService.updateNews(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate (hide) news article")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.deleteNews(id);
        return ResponseEntity.noContent().build();
    }
}
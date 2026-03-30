package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.*;
import synamyk.service.NewsService;
import synamyk.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Tag(name = "Feed", description = "Лента: рейтинг и новости")
public class FeedController {

    private final RatingService ratingService;
    private final NewsService newsService;

    // ===== RATING =====

    /**
     * Get list of all tests for the rating filter selector.
     */
    @GetMapping("/rating/filters")
    @Operation(summary = "Get tests available as rating filter options")
    public ResponseEntity<List<TestListResponse>> getRatingFilters() {
        return ResponseEntity.ok(ratingService.getFilterOptions());
    }

    /**
     * Get leaderboard for a specific test.
     * Score = best correctAnswers from a single completed session.
     */
    @GetMapping("/rating/{testId}")
    @Operation(summary = "Get rating leaderboard for a test")
    public ResponseEntity<RatingResponse> getRating(@PathVariable Long testId) {
        return ResponseEntity.ok(ratingService.getRatingByTest(testId));
    }

    // ===== NEWS =====

    /**
     * Get paginated news list (title, cover image, preview, date).
     */
    @GetMapping("/news")
    @Operation(summary = "Get news list")
    public ResponseEntity<List<NewsListResponse>> getNewsList() {
        return ResponseEntity.ok(newsService.getNewsList());
    }

    /**
     * Get full news article.
     */
    @GetMapping("/news/{id}")
    @Operation(summary = "Get news article detail")
    public ResponseEntity<NewsDetailResponse> getNewsDetail(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.getNewsDetail(id));
    }
}
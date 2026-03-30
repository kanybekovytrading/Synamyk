package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.AnalyticsChartResponse;
import synamyk.dto.TestListResponse;
import synamyk.dto.VideoLessonResponse;
import synamyk.enums.AnalyticsPeriod;
import synamyk.repo.UserRepository;
import synamyk.service.AnalyticsService;
import synamyk.service.RatingService;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Анализ: chart data and video lessons")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RatingService ratingService;
    private final UserRepository userRepository;

    /**
     * GET /api/analytics/chart?testId=&period=WEEK
     * testId is optional; omitting it means "Основной тест" (all tests combined).
     */
    @GetMapping("/chart")
    @Operation(summary = "Get analytics chart for current user")
    public ResponseEntity<AnalyticsChartResponse> getChart(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long testId,
            @RequestParam(defaultValue = "WEEK") AnalyticsPeriod period) {

        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(analyticsService.getChart(userId, testId, period));
    }

    /**
     * GET /api/analytics/filters
     * Returns the list of tests available as filter options (reuses rating filter list).
     */
    @GetMapping("/filters")
    @Operation(summary = "Get test list for analytics filter")
    public ResponseEntity<List<TestListResponse>> getFilters() {
        return ResponseEntity.ok(ratingService.getFilterOptions());
    }

    /**
     * GET /api/analytics/videos?testId=
     * Returns video lessons, optionally filtered by test.
     */
    @GetMapping("/videos")
    @Operation(summary = "Get video lessons")
    public ResponseEntity<List<VideoLessonResponse>> getVideos(
            @RequestParam(required = false) Long testId) {
        return ResponseEntity.ok(analyticsService.getVideos(testId));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
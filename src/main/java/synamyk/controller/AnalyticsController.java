package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import synamyk.util.LangResolver;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Анализ", description = "График аналитики прохождения тестов и видеоуроки")
@SecurityRequirement(name = "Bearer")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final RatingService ratingService;
    private final UserRepository userRepository;
    private final LangResolver langResolver;

    @GetMapping("/chart")
    @Operation(
            summary = "График аналитики текущего пользователя.",
            description = "Возвращает данные для построения графика баллов за выбранный период. " +
                    "**testId** не обязателен — без него агрегируются все тесты. " +
                    "Каждая точка графика — сумма правильных ответов за день. " +
                    "Дни без активности возвращаются с баллом 0. " +
                    "**changePercent** — прирост/спад баллов по сравнению с предыдущим таким же периодом."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные графика"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    public ResponseEntity<AnalyticsChartResponse> getChart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID теста (не обязательно). Без него — все тесты вместе.")
            @RequestParam(required = false) Long testId,
            @Parameter(description = "Период: WEEK (7д), MONTH (30д), THREE_MONTHS (90д), SIX_MONTHS (180д), YEAR (365д)")
            @RequestParam(defaultValue = "WEEK") AnalyticsPeriod period) {
        Long userId = resolveUserId(userDetails);
        String lang = langResolver.resolve(userDetails);
        return ResponseEntity.ok(analyticsService.getChart(userId, testId, period, lang));
    }

    @GetMapping("/filters")
    @Operation(
            summary = "Список тестов для фильтра аналитики",
            description = "Возвращает все активные тесты для выпадающего списка на экране «Анализ»."
    )
    @ApiResponse(responseCode = "200", description = "Список тестов")
    public ResponseEntity<List<TestListResponse>> getFilters(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ratingService.getFilterOptions(langResolver.resolve(userDetails)));
    }

    @GetMapping("/videos")
    @Operation(
            summary = "Список видеоуроков",
            description = "Возвращает видеоуроки, опционально отфильтрованные по тесту " +
                    "Видео хранятся как ссылки на YouTube — поле `videoUrl` " +
                    "Превью — поле `thumbnailUrl` (загружается через POST /api/upload)"
    )
    @ApiResponse(responseCode = "200", description = "Список видеоуроков")
    public ResponseEntity<List<VideoLessonResponse>> getVideos(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "ID теста для фильтрации (не обязательно)")
            @RequestParam(required = false) Long testId) {
        return ResponseEntity.ok(analyticsService.getVideos(testId, langResolver.resolve(userDetails)));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"))
                .getId();
    }
}
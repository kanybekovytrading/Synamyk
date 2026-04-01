package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.AnalyticsChartResponse;
import synamyk.dto.ChartPointResponse;
import synamyk.dto.VideoLessonResponse;
import synamyk.dto.admin.CreateVideoLessonRequest;
import synamyk.entities.Test;
import synamyk.entities.VideoLesson;
import synamyk.enums.AnalyticsPeriod;
import synamyk.repo.TestRepository;
import synamyk.repo.TestSessionRepository;
import synamyk.repo.VideoLessonRepository;
import synamyk.util.L10n;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TestSessionRepository sessionRepository;
    private final TestRepository testRepository;
    private final VideoLessonRepository videoLessonRepository;
    private final MinioService minioService;

    /**
     * Returns chart analytics for the given user.
     * When testId is null — aggregates across all tests ("Основной тест").
     */
    public AnalyticsChartResponse getChart(Long userId, Long testId, AnalyticsPeriod period, String lang) {
        int days = periodToDays(period);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentFrom = now.minusDays(days).with(LocalTime.MIN);
        LocalDateTime previousFrom = currentFrom.minusDays(days);
        LocalDateTime previousTo = currentFrom.minusNanos(1);

        // Scores for current and previous periods
        long currentTotal = sessionRepository.findTotalScore(userId, testId, currentFrom, now);
        long previousTotal = sessionRepository.findTotalScore(userId, testId, previousFrom, previousTo);

        Double changePercent = null;
        if (previousTotal > 0) {
            changePercent = (double) (currentTotal - previousTotal) / previousTotal * 100.0;
            changePercent = Math.round(changePercent * 100.0) / 100.0; // 2 decimal places
        } else if (currentTotal > 0) {
            changePercent = 100.0; // went from 0 to something
        }

        // Chart points: one per day in current period, filling gaps with 0
        List<Object[]> rawPoints = sessionRepository.findDailyScores(userId, testId, currentFrom, now);
        Map<LocalDate, Long> scoreByDate = new HashMap<>();
        for (Object[] row : rawPoints) {
            LocalDate date = ((Date) row[0]).toLocalDate();
            long score = ((Number) row[1]).longValue();
            scoreByDate.put(date, score);
        }

        List<ChartPointResponse> points = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = now.toLocalDate().minusDays(i);
            points.add(ChartPointResponse.builder()
                    .date(date)
                    .score(scoreByDate.getOrDefault(date, 0L))
                    .build());
        }

        // Test title
        String testTitle = "Основной тест";
        Long resolvedTestId = null;
        if (testId != null) {
            Test test = testRepository.findById(testId)
                    .orElseThrow(() -> new RuntimeException("Test not found"));
            testTitle = L10n.pick(test.getTitle(), test.getTitleKy(), lang);
            resolvedTestId = testId;
        }

        return AnalyticsChartResponse.builder()
                .testId(resolvedTestId)
                .testTitle(testTitle)
                .totalScore(currentTotal)
                .changePercent(changePercent)
                .period(period)
                .points(points)
                .build();
    }

    /**
     * Returns video lessons, optionally filtered by test.
     * When testId is null — returns all active lessons.
     */
    public List<VideoLessonResponse> getVideos(Long testId, String lang) {
        List<VideoLesson> lessons = testId != null
                ? videoLessonRepository.findByTestIdAndActiveTrueOrderByOrderIndexAsc(testId)
                : videoLessonRepository.findByActiveTrueOrderByOrderIndexAsc();

        return lessons.stream().map(l -> toResponse(l, lang)).toList();
    }

    @Transactional
    public VideoLessonResponse createVideo(CreateVideoLessonRequest request) {
        VideoLesson.VideoLessonBuilder<?, ?> builder = VideoLesson.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .videoUrl(request.getVideoUrl())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .active(true);

        if (request.getTestId() != null) {
            Test test = testRepository.findById(request.getTestId())
                    .orElseThrow(() -> new RuntimeException("Test not found"));
            builder.test(test);
        }

        return toResponse(videoLessonRepository.save(builder.build()), "RU");
    }

    @Transactional
    public VideoLessonResponse updateVideo(Long id, CreateVideoLessonRequest request) {
        VideoLesson lesson = videoLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video lesson not found"));

        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setThumbnailUrl(request.getThumbnailUrl());
        lesson.setVideoUrl(request.getVideoUrl());
        if (request.getOrderIndex() != null) lesson.setOrderIndex(request.getOrderIndex());

        if (request.getTestId() != null) {
            Test test = testRepository.findById(request.getTestId())
                    .orElseThrow(() -> new RuntimeException("Test not found"));
            lesson.setTest(test);
        } else {
            lesson.setTest(null);
        }

        return toResponse(videoLessonRepository.save(lesson),"RU");
    }

    @Transactional
    public void deleteVideo(Long id) {
        VideoLesson lesson = videoLessonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video lesson not found"));
        lesson.setActive(false);
        videoLessonRepository.save(lesson);
    }

    private int periodToDays(AnalyticsPeriod period) {
        return switch (period) {
            case WEEK -> 7;
            case MONTH -> 30;
            case THREE_MONTHS -> 90;
            case SIX_MONTHS -> 180;
            case YEAR -> 365;
        };
    }

    private VideoLessonResponse toResponse(VideoLesson l, String lang) {
        return VideoLessonResponse.builder()
                .id(l.getId())
                .title(L10n.pick(l.getTitle(), l.getTitleKy(), lang))
                .description(L10n.pick(l.getDescription(), l.getDescriptionKy(), lang))
                .thumbnailUrl(minioService.presign(l.getThumbnailUrl()))
                .videoUrl(l.getVideoUrl())
                .testId(l.getTest() != null ? l.getTest().getId() : null)
                .orderIndex(l.getOrderIndex())
                .build();
    }
}
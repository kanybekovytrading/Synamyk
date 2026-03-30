package synamyk.dto;

import lombok.Builder;
import lombok.Data;
import synamyk.enums.AnalyticsPeriod;

import java.util.List;

@Data
@Builder
public class AnalyticsChartResponse {
    /** null means "Основной тест" (all tests combined). */
    private Long testId;
    private String testTitle;
    private long totalScore;
    /** Percentage change vs previous period. null when no data in previous period. */
    private Double changePercent;
    private AnalyticsPeriod period;
    /** One point per day within the period. Days with no sessions have score = 0. */
    private List<ChartPointResponse> points;
}
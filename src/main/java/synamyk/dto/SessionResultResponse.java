package synamyk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SessionResultResponse {
    private Long sessionId;
    private String subTestTitle;
    private String levelName;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Integer skippedAnswers;
    private Integer totalPoints;
    private Integer earnedPoints;
    private Integer percentage;
    private Long timeTakenSeconds;
    private String motivationalMessage;
    private List<QuestionResultItem> questionResults;

    @Data
    @Builder
    public static class QuestionResultItem {
        private Long questionId;
        private Integer index;          // 1-based for display
        private Boolean isCorrect;
        private Boolean isSkipped;
        private Integer pointValue;
        private List<Long> selectedOptionIds;
        private List<Long> correctOptionIds;
    }
}
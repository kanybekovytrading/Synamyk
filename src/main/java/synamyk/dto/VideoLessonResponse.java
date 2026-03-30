package synamyk.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoLessonResponse {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String videoUrl;
    private Long testId;
    private Integer orderIndex;
}
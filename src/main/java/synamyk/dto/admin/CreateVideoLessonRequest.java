package synamyk.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateVideoLessonRequest {

    @NotBlank
    private String title;

    private String description;

    private String thumbnailUrl;

    @NotBlank
    private String videoUrl;

    /** Optional: associate lesson with a test for filtered display. */
    private Long testId;

    private Integer orderIndex = 0;
}
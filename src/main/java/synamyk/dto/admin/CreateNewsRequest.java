package synamyk.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateNewsRequest {
    @NotBlank
    private String title;

    private String coverImageUrl;

    @NotBlank
    private String content;

    @NotNull
    private LocalDateTime publishedAt;
}
package synamyk.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NewsDetailResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String content;
    private LocalDateTime publishedAt;
}
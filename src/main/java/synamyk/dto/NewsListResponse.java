package synamyk.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NewsListResponse {
    private Long id;
    private String title;
    private String coverImageUrl;
    private String preview;   // first ~150 chars of content
    private LocalDateTime publishedAt;
}
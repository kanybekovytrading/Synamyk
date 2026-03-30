package synamyk.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingEntryResponse {
    private Integer rank;
    private Long userId;
    private String fullName;
    private Integer score;
}
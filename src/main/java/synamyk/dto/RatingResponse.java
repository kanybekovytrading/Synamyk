package synamyk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RatingResponse {
    private Long testId;
    private String testTitle;
    private List<RatingEntryResponse> entries;
}
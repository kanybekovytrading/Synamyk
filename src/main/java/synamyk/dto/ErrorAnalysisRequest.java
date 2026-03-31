package synamyk.dto;

import lombok.Data;

import java.util.List;

@Data
public class ErrorAnalysisRequest {
    private List<Long> questionIds;
}
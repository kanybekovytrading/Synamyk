package synamyk.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ChartPointResponse {
    private LocalDate date;
    private long score;
}
package synamyk.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeRegionRequest {
    @NotNull
    private Long regionId;
}
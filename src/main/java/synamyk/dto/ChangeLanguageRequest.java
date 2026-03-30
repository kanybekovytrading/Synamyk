package synamyk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ChangeLanguageRequest {
    /** Accepted values: RU or KY */
    @NotBlank
    @Pattern(regexp = "^(RU|KY)$", message = "Language must be RU or KY")
    private String language;
}
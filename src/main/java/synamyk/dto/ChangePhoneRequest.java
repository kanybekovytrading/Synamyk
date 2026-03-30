package synamyk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePhoneRequest {
    @NotBlank
    private String oldPhone;
    @NotBlank
    private String newPhone;
}
package synamyk.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmPhoneChangeRequest {
    @NotBlank
    private String newPhone;
    @NotBlank
    private String code;
}
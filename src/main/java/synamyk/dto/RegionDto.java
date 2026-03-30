package synamyk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionDto {
    private Long id;
    private String name;
    private String nameKy;
}
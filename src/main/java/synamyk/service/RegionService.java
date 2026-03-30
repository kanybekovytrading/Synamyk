package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synamyk.dto.RegionDto;
import synamyk.repo.RegionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionService {

    private final RegionRepository regionRepository;

    public List<RegionDto> getAllRegions() {
        return regionRepository.findAll()
                .stream()
                .map(r -> new RegionDto(r.getId(), r.getName(), r.getNameKy()))
                .toList();
    }
}
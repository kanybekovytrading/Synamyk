package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.VideoLessonResponse;
import synamyk.dto.admin.CreateVideoLessonRequest;
import synamyk.service.AnalyticsService;

@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
@Tag(name = "Admin - Videos", description = "Admin: create and manage video lessons")
public class AdminVideoController {

    private final AnalyticsService analyticsService;

    @PostMapping
    @Operation(summary = "Create video lesson")
    public ResponseEntity<VideoLessonResponse> create(@Valid @RequestBody CreateVideoLessonRequest request) {
        return ResponseEntity.ok(analyticsService.createVideo(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update video lesson")
    public ResponseEntity<VideoLessonResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateVideoLessonRequest request) {
        return ResponseEntity.ok(analyticsService.updateVideo(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate (hide) video lesson")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        analyticsService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }
}
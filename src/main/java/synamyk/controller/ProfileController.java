package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.*;
import synamyk.repo.UserRepository;
import synamyk.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile: view, edit, settings, delete account")
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user profile with stats")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(profileService.getProfile(resolveUserId(userDetails)));
    }

    @PutMapping
    @Operation(summary = "Update profile (name, bio, avatar)")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(resolveUserId(userDetails), request));
    }

    @PostMapping("/change-phone/request")
    @Operation(summary = "Request phone change: sends OTP to the new phone")
    public ResponseEntity<OtpSendResponse> requestPhoneChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePhoneRequest request) {
        return ResponseEntity.ok(profileService.requestPhoneChange(resolveUserId(userDetails), request));
    }

    @PostMapping("/change-phone/confirm")
    @Operation(summary = "Confirm phone change with OTP code")
    public ResponseEntity<ApiResponse> confirmPhoneChange(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ConfirmPhoneChangeRequest request) {
        return ResponseEntity.ok(profileService.confirmPhoneChange(resolveUserId(userDetails), request));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(profileService.changePassword(resolveUserId(userDetails), request));
    }

    @PutMapping("/change-region")
    @Operation(summary = "Change region")
    public ResponseEntity<ApiResponse> changeRegion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangeRegionRequest request) {
        return ResponseEntity.ok(profileService.changeRegion(resolveUserId(userDetails), request));
    }

    @PutMapping("/language")
    @Operation(summary = "Change interface language (RU or KY)")
    public ResponseEntity<ApiResponse> changeLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangeLanguageRequest request) {
        return ResponseEntity.ok(profileService.changeLanguage(resolveUserId(userDetails), request));
    }

    @DeleteMapping
    @Operation(summary = "Delete account (soft delete — account is deactivated)")
    public ResponseEntity<ApiResponse> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(profileService.deleteAccount(resolveUserId(userDetails)));
    }

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByPhone(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
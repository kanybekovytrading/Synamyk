package synamyk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.*;
import synamyk.entities.OTPCode;
import synamyk.entities.Region;
import synamyk.entities.User;
import synamyk.repo.OtpCodeRepository;
import synamyk.repo.RegionRepository;
import synamyk.repo.TestSessionRepository;
import synamyk.repo.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final TestSessionRepository sessionRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmsProService smsProService;

    public ProfileResponse getProfile(Long userId) {
        User user = findUser(userId);

        long completedTests = sessionRepository.countByUserIdAndStatus(userId, synamyk.entities.TestSession.SessionStatus.COMPLETED);
        long totalScore = sessionRepository.sumCorrectAnswersByUserId(userId);
        long referrals = userRepository.countByReferredById(userId);

        return ProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .language(user.getLanguage())
                .referralCode(user.getReferralCode())
                .regionId(user.getRegion() != null ? user.getRegion().getId() : null)
                .regionName(user.getRegion() != null ? user.getRegion().getName() : null)
                .completedTests(completedTests)
                .totalScore(totalScore)
                .referrals(referrals)
                .build();
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        userRepository.save(user);
        return getProfile(userId);
    }

    /**
     * Step 1: verify old phone, check new phone is free, send OTP to new phone.
     */
    @Transactional
    public OtpSendResponse requestPhoneChange(Long userId, ChangePhoneRequest request) {
        User user = findUser(userId);
        String oldPhone = formatPhone(request.getOldPhone());
        String newPhone = formatPhone(request.getNewPhone());

        if (!user.getPhone().equals(oldPhone)) {
            throw new RuntimeException("Old phone number does not match.");
        }
        if (userRepository.existsByPhone(newPhone)) {
            throw new RuntimeException("This phone number is already registered.");
        }

        return smsProService.sendOtp(newPhone, OTPCode.OtpType.PHONE_CHANGE);
    }

    /**
     * Step 2: verify OTP sent to new phone, then switch user's phone.
     */
    @Transactional
    public ApiResponse confirmPhoneChange(Long userId, ConfirmPhoneChangeRequest request) {
        User user = findUser(userId);
        String newPhone = formatPhone(request.getNewPhone());

        if (userRepository.existsByPhone(newPhone)) {
            throw new RuntimeException("This phone number is already registered.");
        }

        List<OTPCode> otpCodes = otpCodeRepository
                .findByPhoneAndTypeAndVerifiedFalseOrderByCreatedAtDesc(newPhone, OTPCode.OtpType.PHONE_CHANGE);

        if (otpCodes.isEmpty()) {
            throw new RuntimeException("OTP not found or already used.");
        }

        OTPCode otp = otpCodes.get(0);

        if (otp.isExpired()) {
            throw new RuntimeException("OTP has expired. Please request a new code.");
        }
        if (!otp.getCode().equals(request.getCode())) {
            throw new RuntimeException("Invalid verification code.");
        }

        otp.setVerified(true);
        otp.setVerifiedAt(LocalDateTime.now());
        otp.setUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otp);

        user.setPhone(newPhone);
        userRepository.save(user);

        log.info("Phone changed successfully for user {}: {}", userId, newPhone);
        return ApiResponse.builder().success(true).message("Phone number updated successfully.").build();
    }

    @Transactional
    public ApiResponse changePassword(Long userId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New passwords do not match.");
        }

        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ApiResponse.builder().success(true).message("Password changed successfully.").build();
    }

    @Transactional
    public ApiResponse changeRegion(Long userId, ChangeRegionRequest request) {
        User user = findUser(userId);
        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new RuntimeException("Region not found."));
        user.setRegion(region);
        userRepository.save(user);
        return ApiResponse.builder().success(true).message("Region updated successfully.").build();
    }

    @Transactional
    public ApiResponse changeLanguage(Long userId, ChangeLanguageRequest request) {
        User user = findUser(userId);
        user.setLanguage(request.getLanguage());
        userRepository.save(user);
        return ApiResponse.builder().success(true).message("Language updated successfully.").build();
    }

    /**
     * Soft-deletes the account: sets active = false.
     * The user will no longer be able to log in (isEnabled() returns false).
     */
    @Transactional
    public ApiResponse deleteAccount(Long userId) {
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
        log.info("Account soft-deleted for user {}", userId);
        return ApiResponse.builder().success(true).message("Account deleted successfully.").build();
    }

    /**
     * Generates a unique 8-character alphanumeric referral code and assigns it to the user.
     * Called once after registration if the user doesn't have one yet.
     */
    @Transactional
    public void ensureReferralCode(User user) {
        if (user.getReferralCode() != null) return;
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        user.setReferralCode(code);
        userRepository.save(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    private String formatPhone(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) cleaned = "996" + cleaned.substring(1);
        if (!cleaned.startsWith("996")) cleaned = "996" + cleaned;
        return cleaned;
    }
}
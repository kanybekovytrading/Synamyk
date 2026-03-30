package synamyk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.config.JwtService;
import synamyk.dto.*;
import synamyk.entities.OTPCode;
import synamyk.entities.Region;
import synamyk.entities.User;
import synamyk.enums.Role;
import synamyk.repo.OtpCodeRepository;
import synamyk.repo.RegionRepository;
import synamyk.repo.UserRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpRepository;
    private final RegionRepository regionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SmsProService smsService;
    private final ProfileService profileService;

    /**
     * Step 1 of registration: create account with phone + password, send OTP.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        String formattedPhone = formatPhone(request.getPhone());

        if (userRepository.existsByPhone(formattedPhone)) {
            throw new RuntimeException("Phone number is already registered.");
        }

        User user = User.builder()
                .phone(formattedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .phoneVerified(false)
                .active(true)
                .build();

        userRepository.save(user);
        profileService.ensureReferralCode(user);

        // Send OTP
        smsService.sendOtp(formattedPhone, OTPCode.OtpType.REGISTRATION);

        String token = jwtService.generateToken(user, user.getPhone());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Step 3 of registration: save firstName, lastName, region after OTP verification.
     */
    @Transactional
    public AuthResponse completeProfile(CompleteProfileRequest request) {
        String formattedPhone = formatPhone(request.getPhone());

        User user = userRepository.findByPhone(formattedPhone)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!user.getPhoneVerified()) {
            throw new RuntimeException("Phone number is not verified. Please verify your OTP first.");
        }

        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new RuntimeException("Region not found."));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRegion(region);
        userRepository.save(user);

        String token = jwtService.generateToken(user, user.getPhone());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String formattedPhone = formatPhone(request.getPhone());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(formattedPhone, request.getPassword())
        );

        User user = userRepository.findByPhone(formattedPhone)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (!user.getPhoneVerified()) {
            throw new RuntimeException("Phone number not verified.");
        }

        String token = jwtService.generateToken(user, user.getPhone());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String formattedPhone = formatPhone(request.getPhone());
        log.info("Resetting password for phone: {}", formattedPhone);

        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);

        OTPCode verifiedOtp = otpRepository.findAll()
                .stream()
                .filter(otp -> otp.getPhone().equals(formattedPhone))
                .filter(otp -> otp.getType() == OTPCode.OtpType.PASSWORD_RESET)
                .filter(otp -> Boolean.TRUE.equals(otp.getVerified()))
                .filter(otp -> Boolean.FALSE.equals(otp.getUsed()))
                .filter(otp -> otp.getVerifiedAt() != null)
                .filter(otp -> otp.getVerifiedAt().isAfter(tenMinutesAgo))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("OTP not verified, already used, or expired."));

        User user = userRepository.findByPhone(formattedPhone)
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verifiedOtp.setUsed(true);
        verifiedOtp.setUsedAt(LocalDateTime.now());
        otpRepository.save(verifiedOtp);

        log.info("Password reset successfully for user: {}", user.getId());
    }

    private String formatPhone(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) cleaned = "996" + cleaned.substring(1);
        if (!cleaned.startsWith("996")) cleaned = "996" + cleaned;
        return cleaned;
    }
}
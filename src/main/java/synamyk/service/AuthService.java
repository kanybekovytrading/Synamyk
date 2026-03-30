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
import synamyk.exception.AppException;
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
    private final RefreshTokenService refreshTokenService;

    /**
     * Step 1 of registration: create account with phone + password, send OTP.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException("Пароли не совпадают.", "Сырсөздөр дал келбейт.");
        }

        String formattedPhone = formatPhone(request.getPhone());

        if (userRepository.existsByPhone(formattedPhone)) {
            throw new AppException("Этот номер уже зарегистрирован.", "Бул номер катталган.");
        }

        User user = User.builder()
                .phone(formattedPhone)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .language("RU")
                .phoneVerified(false)
                .active(true)
                .build();

        userRepository.save(user);
        profileService.ensureReferralCode(user);

        // Send OTP
        smsService.sendOtp(formattedPhone, OTPCode.OtpType.REGISTRATION);

        String token = jwtService.generateToken(user, user.getPhone());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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
                .orElseThrow(() -> new AppException("Пользователь не найден.", "Колдонуучу табылган жок."));

        if (!user.getPhoneVerified()) {
            throw new AppException(
                    "Номер телефона не подтверждён. Пройдите верификацию OTP.",
                    "Телефон номери тастыкталган жок. OTP аркылуу ырастаңыз.");
        }

        Region region = regionRepository.findById(request.getRegionId())
                .orElseThrow(() -> new AppException("Регион не найден.", "Аймак табылган жок."));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRegion(region);
        userRepository.save(user);

        String token = jwtService.generateToken(user, user.getPhone());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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
                .orElseThrow(() -> new AppException("Пользователь не найден.", "Колдонуучу табылган жок."));

        if (!user.getPhoneVerified()) {
            throw new AppException("Номер телефона не подтверждён.", "Телефон номери тастыкталган жок.");
        }

        String token = jwtService.generateToken(user, user.getPhone());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
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
                .orElseThrow(() -> new AppException(
                        "OTP не подтверждён, уже использован или истёк.",
                        "OTP тастыкталган жок, колдонулган же мөөнөтү өткөн."));

        User user = userRepository.findByPhone(formattedPhone)
                .orElseThrow(() -> new AppException("Пользователь не найден.", "Колдонуучу табылган жок."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        verifiedOtp.setUsed(true);
        verifiedOtp.setUsedAt(LocalDateTime.now());
        otpRepository.save(verifiedOtp);

        log.info("Password reset successfully for user: {}", user.getId());
    }

    public AuthResponse refreshToken(String refreshToken) {
        return refreshTokenService.refresh(refreshToken);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private String formatPhone(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) cleaned = "996" + cleaned.substring(1);
        if (!cleaned.startsWith("996")) cleaned = "996" + cleaned;
        return cleaned;
    }
}
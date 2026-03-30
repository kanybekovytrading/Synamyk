package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.config.JwtService;
import synamyk.dto.AuthResponse;
import synamyk.entities.RefreshToken;
import synamyk.entities.User;
import synamyk.exception.AppException;
import synamyk.repo.RefreshTokenRepository;
import synamyk.repo.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Validates token, rotates it (revokes old, issues new), and returns new access + refresh tokens.
     */
    @Transactional
    public AuthResponse refresh(String tokenValue) {
        RefreshToken stored = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new AppException("Refresh-токен не найден.", "Refresh-токен табылган жок."));

        if (Boolean.TRUE.equals(stored.getRevoked())) {
            throw new AppException("Refresh-токен отозван.", "Refresh-токен жокко чыгарылган.");
        }
        if (stored.isExpired()) {
            throw new AppException("Refresh-токен истёк.", "Refresh-токендин мөөнөтү өттү.");
        }

        User user = stored.getUser();

        // Rotate: revoke the used token, issue a new one
        refreshTokenRepository.revokeByToken(tokenValue);
        RefreshToken newRefreshToken = createRefreshToken(user);

        String accessToken = jwtService.generateToken(user, user.getPhone());

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(newRefreshToken.getToken())
                .userId(user.getId())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
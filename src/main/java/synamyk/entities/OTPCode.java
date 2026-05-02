package synamyk.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class OTPCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean used = false;

    private LocalDateTime usedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime verifiedAt;

    @Column(columnDefinition = "integer default 0 not null")
    @Builder.Default
    private Integer attempts = 0;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public enum OtpType {
        REGISTRATION,
        PASSWORD_RESET,
        PHONE_CHANGE
    }
}
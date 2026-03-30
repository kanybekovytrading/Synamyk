package synamyk.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String phone;
    private String firstName;
    private String lastName;
    private String bio;
    private String avatarUrl;
    private String language;
    private String referralCode;
    private Long regionId;
    private String regionName;

    /** Number of completed test sessions. */
    private long completedTests;
    /** Sum of correctAnswers across all completed sessions. */
    private long totalScore;
    /** Number of users referred by this user. */
    private long referrals;
}
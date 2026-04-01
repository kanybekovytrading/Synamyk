package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synamyk.dto.RatingEntryResponse;
import synamyk.dto.RatingResponse;
import synamyk.dto.TestListResponse;
import synamyk.entities.Test;
import synamyk.repo.TestRepository;
import synamyk.repo.TestSessionRepository;
import synamyk.util.L10n;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final TestSessionRepository sessionRepository;
    private final TestRepository testRepository;
    private final MinioService minioService;

    /**
     * Returns leaderboard for a specific test.
     * Score = best (max) correctAnswers from a single completed session
     * across all sub-tests of that test.
     */
    public RatingResponse getRatingByTest(Long testId, String lang) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        List<Object[]> rows = sessionRepository.findRankingByTestId(testId);

        List<RatingEntryResponse> entries = new ArrayList<>();
        int rank = 1;
        int prevScore = -1;
        int displayRank = 1;

        for (Object[] row : rows) {
            Long userId    = ((Number) row[0]).longValue();
            String firstName = (String) row[1];
            String lastName  = (String) row[2];
            String phone     = (String) row[3];
            int score      = ((Number) row[4]).intValue();

            // Handle ties: same score → same rank
            if (score != prevScore) {
                displayRank = rank;
            }

            String fullName = buildFullName(firstName, lastName, phone);

            entries.add(RatingEntryResponse.builder()
                    .rank(displayRank)
                    .userId(userId)
                    .fullName(fullName)
                    .score(score)
                    .build());

            prevScore = score;
            rank++;
        }

        return RatingResponse.builder()
                .testId(test.getId())
                .testTitle(L10n.pick(test.getTitle(), test.getTitleKy(), lang))
                .entries(entries)
                .build();
    }

    /**
     * Returns list of all active tests available as filter options.
     */
    public List<TestListResponse> getFilterOptions(String lang) {
        return testRepository.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(t -> TestListResponse.builder()
                        .id(t.getId())
                        .title(L10n.pick(t.getTitle(), t.getTitleKy(), lang))
                        .description(L10n.pick(t.getDescription(), t.getDescriptionKy(), lang))
                        .iconUrl(minioService.presign(t.getIconUrl()))
                        .price(t.getPrice())
                        .subTestCount(0)
                        .build())
                .toList();
    }

    private String buildFullName(String firstName, String lastName, String phone) {
        if (firstName == null && lastName == null) {
            // Mask phone: +996700123456 → +996700***456
            if (phone != null && phone.length() > 6) {
                return phone.substring(0, phone.length() - 6) + "***" + phone.substring(phone.length() - 3);
            }
            return phone != null ? phone : "—";
        }
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
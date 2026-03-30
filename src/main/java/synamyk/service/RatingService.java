package synamyk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synamyk.dto.RatingEntryResponse;
import synamyk.dto.RatingResponse;
import synamyk.dto.TestListResponse;
import synamyk.entities.Test;
import synamyk.repo.TestRepository;
import synamyk.repo.TestSessionRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final TestSessionRepository sessionRepository;
    private final TestRepository testRepository;

    /**
     * Returns leaderboard for a specific test.
     * Score = best (max) correctAnswers from a single completed session
     * across all sub-tests of that test.
     */
    public RatingResponse getRatingByTest(Long testId) {
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
            int score      = ((Number) row[3]).intValue();

            // Handle ties: same score → same rank
            if (score != prevScore) {
                displayRank = rank;
            }

            String fullName = buildFullName(firstName, lastName);

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
                .testTitle(test.getTitle())
                .entries(entries)
                .build();
    }

    /**
     * Returns list of all active tests available as filter options.
     */
    public List<TestListResponse> getFilterOptions() {
        return testRepository.findByActiveTrueOrderByCreatedAtAsc().stream()
                .map(t -> TestListResponse.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .iconUrl(t.getIconUrl())
                        .price(t.getPrice())
                        .subTestCount(0)
                        .build())
                .toList();
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return "—";
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}
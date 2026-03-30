package synamyk.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import synamyk.entities.TestSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Long> {

    long countByUserIdAndStatus(Long userId, TestSession.SessionStatus status);

    @Query("SELECT COALESCE(SUM(s.earnedPoints), 0) FROM TestSession s WHERE s.user.id = :userId AND s.status = 'COMPLETED'")
    long sumCorrectAnswersByUserId(@Param("userId") Long userId);

    /** Find active (IN_PROGRESS) session for a user + sub-test. */
    Optional<TestSession> findByUserIdAndSubTestIdAndStatus(
            Long userId, Long subTestId, TestSession.SessionStatus status);

    /** Find any resumable session (IN_PROGRESS or PAUSED) for a user + sub-test. */
    @Query("SELECT s FROM TestSession s WHERE s.user.id = :userId AND s.subTest.id = :subTestId " +
           "AND s.status IN ('IN_PROGRESS', 'PAUSED') ORDER BY s.createdAt DESC")
    List<TestSession> findResumable(@Param("userId") Long userId, @Param("subTestId") Long subTestId);

    List<TestSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<TestSession> findByUserIdAndSubTestIdOrderByCreatedAtDesc(Long userId, Long subTestId);

    /**
     * Leaderboard: for each user return their best (max) correctAnswers
     * across all completed sessions of sub-tests belonging to the given test.
     * Returns Object[]: [userId, firstName, lastName, bestScore]
     */
    @Query("""
            SELECT s.user.id,
                   s.user.firstName,
                   s.user.lastName,
                   MAX(s.earnedPoints)
            FROM TestSession s
            WHERE s.subTest.test.id = :testId
              AND s.status = 'COMPLETED'
            GROUP BY s.user.id, s.user.firstName, s.user.lastName
            ORDER BY MAX(s.earnedPoints) DESC
            """)
    List<Object[]> findRankingByTestId(@Param("testId") Long testId);

    /**
     * Daily chart data for analytics: returns [date (LocalDate), sumScore (Long)] rows.
     * When testId is null, aggregates across all tests.
     */
    @Query(value = """
            SELECT DATE(ts.completed_at) AS day,
                   SUM(ts.earned_points) AS score
            FROM test_sessions ts
            WHERE ts.user_id = :userId
              AND ts.status = 'COMPLETED'
              AND ts.completed_at >= :from
              AND ts.completed_at <= :to
              AND (:testId IS NULL OR ts.sub_test_id IN (
                      SELECT id FROM sub_tests WHERE test_id = :testId))
            GROUP BY DATE(ts.completed_at)
            ORDER BY DATE(ts.completed_at)
            """, nativeQuery = true)
    List<Object[]> findDailyScores(
            @Param("userId") Long userId,
            @Param("testId") Long testId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Total score (sum of correctAnswers) for a user in a time range.
     * When testId is null, sums across all tests.
     */
    @Query(value = """
            SELECT COALESCE(SUM(ts.earned_points), 0)
            FROM test_sessions ts
            WHERE ts.user_id = :userId
              AND ts.status = 'COMPLETED'
              AND ts.completed_at >= :from
              AND ts.completed_at <= :to
              AND (:testId IS NULL OR ts.sub_test_id IN (
                      SELECT id FROM sub_tests WHERE test_id = :testId))
            """, nativeQuery = true)
    Long findTotalScore(
            @Param("userId") Long userId,
            @Param("testId") Long testId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
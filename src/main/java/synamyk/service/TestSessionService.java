package synamyk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import synamyk.dto.*;
import synamyk.entities.*;
import synamyk.repo.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestSessionService {

    private final SubTestRepository subTestRepository;
    private final QuestionRepository questionRepository;
    private final UserTestAccessRepository accessRepository;
    private final TestSessionRepository sessionRepository;
    private final UserAnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ClaudeAiService claudeAiService;

    /**
     * Start or resume a sub-test session.
     */
    @Transactional
    public StartSessionResponse startSession(Long subTestId, Long userId) {
        SubTest subTest = subTestRepository.findById(subTestId)
                .orElseThrow(() -> new RuntimeException("SubTest not found"));

        // Check access
        if (subTest.getIsPaid() && !accessRepository.existsByUserIdAndTestId(userId, subTest.getTest().getId())) {
            throw new RuntimeException("Access denied. Please purchase the test first.");
        }

        // Look for any resumable session (IN_PROGRESS or PAUSED)
        List<TestSession> resumable = sessionRepository.findResumable(userId, subTestId);

        if (!resumable.isEmpty()) {
            TestSession session = resumable.get(0);

            // If timer expired, mark as EXPIRED and start fresh
            if (session.isExpired()) {
                session.setStatus(TestSession.SessionStatus.EXPIRED);
                sessionRepository.save(session);
                log.info("Session expired: sessionId={}", session.getId());
            } else {
                // Resume
                session.setStatus(TestSession.SessionStatus.IN_PROGRESS);
                sessionRepository.save(session);
                log.info("Resuming session: sessionId={}, userId={}, index={}",
                        session.getId(), userId, session.getCurrentIndex());
                int total = questionRepository.findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(subTestId).size();
                return StartSessionResponse.builder()
                        .sessionId(session.getId())
                        .subTestId(subTestId)
                        .subTestTitle(subTest.getTitle())
                        .levelName(subTest.getLevelName())
                        .totalQuestions(total)
                        .durationMinutes(subTest.getDurationMinutes())
                        .expiresAt(session.getExpiresAt())
                        .remainingSeconds(session.getRemainingSeconds())
                        .currentIndex(session.getCurrentIndex())
                        .isResumed(true)
                        .build();
            }
        }

        List<Question> questions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(subTestId);

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions available for this sub-test.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(subTest.getDurationMinutes());

        User user = userRepository.getReferenceById(userId);
        TestSession session = TestSession.builder()
                .user(user)
                .subTest(subTest)
                .status(TestSession.SessionStatus.IN_PROGRESS)
                .currentIndex(0)
                .startedAt(now)
                .expiresAt(expiresAt)
                .build();

        session = sessionRepository.save(session);
        log.info("New session started: sessionId={}, userId={}, subTestId={}", session.getId(), userId, subTestId);

        return StartSessionResponse.builder()
                .sessionId(session.getId())
                .subTestId(subTestId)
                .subTestTitle(subTest.getTitle())
                .levelName(subTest.getLevelName())
                .totalQuestions(questions.size())
                .durationMinutes(subTest.getDurationMinutes())
                .expiresAt(expiresAt)
                .remainingSeconds(session.getRemainingSeconds())
                .currentIndex(0)
                .isResumed(false)
                .build();
    }

    /**
     * Get current question for a session.
     */
    public QuestionForSessionResponse getCurrentQuestion(Long sessionId, Long userId) {
        TestSession session = getActiveSession(sessionId, userId);
        List<Question> questions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());

        int index = session.getCurrentIndex();
        if (index >= questions.size()) {
            throw new RuntimeException("No more questions.");
        }

        Question question = questions.get(index);
        return buildQuestionResponse(question, index, questions.size(), session);
    }

    /**
     * Get question by index.
     */
    public QuestionForSessionResponse getQuestionByIndex(Long sessionId, Long userId, int index) {
        TestSession session = getActiveSession(sessionId, userId);
        List<Question> questions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());

        if (index < 0 || index >= questions.size()) {
            throw new RuntimeException("Invalid question index.");
        }

        Question question = questions.get(index);
        return buildQuestionResponse(question, index, questions.size(), session);
    }

    /**
     * Submit answer for current question and advance to next.
     */
    @Transactional
    public QuestionForSessionResponse submitAnswer(Long sessionId, Long userId, SubmitAnswerRequest request) {
        TestSession session = getActiveSession(sessionId, userId);

        if (session.isExpired()) {
            throw new RuntimeException("Session has expired.");
        }

        List<Question> questions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());

        int currentIndex = session.getCurrentIndex();
        if (currentIndex >= questions.size()) {
            throw new RuntimeException("All questions already answered.");
        }

        Question currentQuestion = questions.get(currentIndex);

        // Validate question matches
        if (!currentQuestion.getId().equals(request.getQuestionId())) {
            throw new RuntimeException("Question mismatch.");
        }

        // Save/update answer
        Optional<UserAnswer> existingAnswer = answerRepository.findBySessionIdAndQuestionId(sessionId, request.getQuestionId());
        UserAnswer answer;

        if (existingAnswer.isPresent()) {
            answer = existingAnswer.get();
        } else {
            answer = UserAnswer.builder()
                    .session(session)
                    .question(currentQuestion)
                    .build();
        }

        boolean isSkipped = request.getSelectedOptionId() == null;
        answer.setIsSkipped(isSkipped);

        if (!isSkipped) {
            AnswerOption selectedOption = currentQuestion.getOptions().stream()
                    .filter(o -> o.getId().equals(request.getSelectedOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Option not found"));
            answer.setSelectedOption(selectedOption);
            answer.setIsCorrect(selectedOption.getIsCorrect());
        } else {
            answer.setSelectedOption(null);
            answer.setIsCorrect(false);
        }

        answerRepository.save(answer);

        // Advance to next question
        int nextIndex = currentIndex + 1;
        session.setCurrentIndex(nextIndex);
        sessionRepository.save(session);

        // If last question, return null (caller will call finish)
        if (nextIndex >= questions.size()) {
            return null; // signals end of test
        }

        Question nextQuestion = questions.get(nextIndex);
        return buildQuestionResponse(nextQuestion, nextIndex, questions.size(), session);
    }

    /**
     * Skip current question and advance to next.
     */
    @Transactional
    public QuestionForSessionResponse skipQuestion(Long sessionId, Long userId) {
        TestSession session = getActiveSession(sessionId, userId);
        List<Question> questions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());
        int currentIndex = session.getCurrentIndex();
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        request.setQuestionId(questions.get(currentIndex).getId());
        request.setSelectedOptionId(null);
        return submitAnswer(sessionId, userId, request);
    }

    /**
     * Finish the test session and return results.
     */
    @Transactional
    public SessionResultResponse finishSession(Long sessionId, Long userId) {
        TestSession session = getActiveSession(sessionId, userId);

        long correct = answerRepository.countBySessionIdAndIsCorrectTrue(sessionId);
        session.setCorrectAnswers((int) correct);
        session.setStatus(TestSession.SessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        sessionRepository.save(session);
        return buildResult(session);
    }

    /**
     * Pause session (user clicked "Прервать тест?" → "Да, хочу").
     * Session remains resumable until timer expires.
     */
    @Transactional
    public void pauseSession(Long sessionId, Long userId) {
        TestSession session = getActiveSession(sessionId, userId);
        session.setStatus(TestSession.SessionStatus.PAUSED);
        sessionRepository.save(session);
        log.info("Session paused: sessionId={}, currentIndex={}", sessionId, session.getCurrentIndex());
    }

    /**
     * Get result for a completed session.
     */
    public SessionResultResponse getResult(Long sessionId, Long userId) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied.");
        }

        return buildResult(session);
    }

    /**
     * AI analysis of selected wrong answers.
     */
    public ErrorAnalysisResponse analyzeErrors(Long sessionId, Long userId, ErrorAnalysisRequest request) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied.");
        }

        List<Question> allQuestions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());

        // Build index map: questionId -> 1-based display index
        Map<Long, Integer> questionIndexMap = IntStream.range(0, allQuestions.size())
                .boxed()
                .collect(Collectors.toMap(i -> allQuestions.get(i).getId(), i -> i + 1));

        List<UserAnswer> wrongAnswers = answerRepository.findBySessionIdOrderByQuestionOrderIndex(sessionId)
                .stream()
                .filter(a -> !a.getIsCorrect() && !a.getIsSkipped())
                .filter(a -> request.getQuestionIds().contains(a.getQuestion().getId()))
                .toList();

        List<ErrorAnalysisResponse.QuestionAnalysis> analyses = new ArrayList<>();

        for (UserAnswer wa : wrongAnswers) {
            Question q = wa.getQuestion();
            List<AnswerOption> options = q.getOptions();

            String userWrongText = wa.getSelectedOption() != null
                    ? wa.getSelectedOption().getLabel() + ". " + wa.getSelectedOption().getText()
                    : "—";

            String correctText = options.stream()
                    .filter(AnswerOption::getIsCorrect)
                    .map(o -> o.getLabel() + ". " + o.getText())
                    .findFirst()
                    .orElse("—");

            List<String> optionTexts = options.stream()
                    .map(o -> o.getLabel() + ". " + o.getText())
                    .toList();

            String explanation = claudeAiService.explainWrongAnswer(q.getText(), optionTexts, userWrongText, correctText);

            int questionIndex = questionIndexMap.getOrDefault(q.getId(), 0);

            analyses.add(ErrorAnalysisResponse.QuestionAnalysis.builder()
                    .questionId(q.getId())
                    .questionIndex(questionIndex)
                    .questionText(q.getText())
                    .wrongAnswer(userWrongText)
                    .correctAnswer(correctText)
                    .explanation(explanation)
                    .build());
        }

        return ErrorAnalysisResponse.builder().analyses(analyses).build();
    }

    // ===== HELPERS =====

    private TestSession getActiveSession(Long sessionId, Long userId) {
        TestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied.");
        }

        if (session.getStatus() != TestSession.SessionStatus.IN_PROGRESS) {
            throw new RuntimeException("Session is not active. Status: " + session.getStatus());
        }

        return session;
    }

    private QuestionForSessionResponse buildQuestionResponse(
            Question question, int index, int total, TestSession session) {

        Optional<UserAnswer> existingAnswer = answerRepository
                .findBySessionIdAndQuestionId(session.getId(), question.getId());

        Long selectedOptionId = existingAnswer
                .flatMap(a -> Optional.ofNullable(a.getSelectedOption()))
                .map(AnswerOption::getId)
                .orElse(null);

        Boolean isSkipped = existingAnswer.map(UserAnswer::getIsSkipped).orElse(false);

        List<AnswerOptionResponse> options = question.getOptions().stream()
                .map(o -> AnswerOptionResponse.builder()
                        .id(o.getId())
                        .label(o.getLabel())
                        .text(o.getText())
                        .orderIndex(o.getOrderIndex())
                        .build())
                .toList();

        return QuestionForSessionResponse.builder()
                .questionId(question.getId())
                .index(index)
                .totalQuestions(total)
                .sectionName(question.getSectionName())
                .text(question.getText())
                .imageUrl(question.getImageUrl())
                .pointValue(question.getPointValue())
                .options(options)
                .remainingSeconds(session.getRemainingSeconds())
                .selectedOptionId(selectedOptionId)
                .isSkipped(isSkipped)
                .build();
    }

    private SessionResultResponse buildResult(TestSession session) {
        List<UserAnswer> answers = answerRepository.findBySessionIdOrderByQuestionOrderIndex(session.getId());
        List<Question> allQuestions = questionRepository
                .findBySubTestIdAndActiveTrueOrderByOrderIndexAsc(session.getSubTest().getId());

        int total = allQuestions.size();
        long correct = answers.stream().filter(UserAnswer::getIsCorrect).count();
        long skipped = answers.stream().filter(UserAnswer::getIsSkipped).count();
        long wrong = answers.stream().filter(a -> !a.getIsCorrect() && !a.getIsSkipped()).count();

        int percentage = total > 0 ? (int) ((correct * 100) / total) : 0;

        long timeTaken = 0;
        if (session.getCompletedAt() != null) {
            timeTaken = java.time.Duration.between(session.getStartedAt(), session.getCompletedAt()).getSeconds();
        }

        String motivation = getMotivationalMessage(percentage);

        List<SessionResultResponse.QuestionResultItem> items = IntStream.range(0, allQuestions.size())
                .mapToObj(idx -> {
                    Question q = allQuestions.get(idx);
                    Optional<UserAnswer> ua = answers.stream()
                            .filter(a -> a.getQuestion().getId().equals(q.getId()))
                            .findFirst();

                    Long correctOptionId = q.getOptions().stream()
                            .filter(AnswerOption::getIsCorrect)
                            .map(AnswerOption::getId)
                            .findFirst().orElse(null);

                    return SessionResultResponse.QuestionResultItem.builder()
                            .questionId(q.getId())
                            .index(idx + 1)
                            .isCorrect(ua.map(UserAnswer::getIsCorrect).orElse(false))
                            .isSkipped(ua.map(UserAnswer::getIsSkipped).orElse(true))
                            .selectedOptionId(ua.flatMap(a -> Optional.ofNullable(a.getSelectedOption()))
                                    .map(AnswerOption::getId).orElse(null))
                            .correctOptionId(correctOptionId)
                            .build();
                })
                .collect(Collectors.toList());

        return SessionResultResponse.builder()
                .sessionId(session.getId())
                .subTestTitle(session.getSubTest().getTitle())
                .levelName(session.getSubTest().getLevelName())
                .totalQuestions(total)
                .correctAnswers((int) correct)
                .wrongAnswers((int) wrong)
                .skippedAnswers((int) skipped)
                .percentage(percentage)
                .timeTakenSeconds(timeTaken)
                .motivationalMessage(motivation)
                .questionResults(items)
                .build();
    }

    private String getMotivationalMessage(int percentage) {
        if (percentage >= 90) return "Отлично! Ты настоящий знаток!";
        if (percentage >= 70) return "Хорошая работа! Продолжай в том же духе!";
        if (percentage >= 50) return "Неплохо! Ещё немного практики и будет отлично!";
        return "Не сдавайся! Каждая попытка делает тебя сильнее!";
    }
}
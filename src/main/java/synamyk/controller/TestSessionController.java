package synamyk.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synamyk.dto.*;
import synamyk.entities.User;
import synamyk.service.TestSessionService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Test Session", description = "Test taking: start, answer, results, AI analysis")
public class TestSessionController {

    private final TestSessionService sessionService;

    /**
     * Start or resume a sub-test session.
     * Returns session info + first question index to start from.
     */
    @PostMapping("/sub-tests/{subTestId}/start")
    @Operation(summary = "Start or resume a sub-test session")
    public ResponseEntity<StartSessionResponse> startSession(
            @PathVariable Long subTestId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.startSession(subTestId, user.getId()));
    }

    /**
     * Get current question for a session.
     */
    @GetMapping("/sessions/{sessionId}/question")
    @Operation(summary = "Get current question")
    public ResponseEntity<QuestionForSessionResponse> getCurrentQuestion(
            @PathVariable Long sessionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.getCurrentQuestion(sessionId, user.getId()));
    }

    /**
     * Get question by index (for navigation within a session).
     */
    @GetMapping("/sessions/{sessionId}/question/{index}")
    @Operation(summary = "Get question by index (0-based)")
    public ResponseEntity<QuestionForSessionResponse> getQuestionByIndex(
            @PathVariable Long sessionId,
            @PathVariable int index,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.getQuestionByIndex(sessionId, user.getId(), index));
    }

    /**
     * Submit answer for current question.
     * Returns next question, or null body if it was the last question.
     */
    @PostMapping("/sessions/{sessionId}/answer")
    @Operation(summary = "Submit answer and get next question")
    public ResponseEntity<QuestionForSessionResponse> submitAnswer(
            @PathVariable Long sessionId,
            @RequestBody SubmitAnswerRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        QuestionForSessionResponse next = sessionService.submitAnswer(sessionId, user.getId(), request);
        if (next == null) {
            return ResponseEntity.noContent().build(); // 204 = last question answered
        }
        return ResponseEntity.ok(next);
    }

    /**
     * Skip current question.
     */
    @PostMapping("/sessions/{sessionId}/skip")
    @Operation(summary = "Skip current question")
    public ResponseEntity<QuestionForSessionResponse> skipQuestion(
            @PathVariable Long sessionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        QuestionForSessionResponse next = sessionService.skipQuestion(sessionId, user.getId());
        if (next == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(next);
    }

    /**
     * Finish test and get results.
     */
    @PostMapping("/sessions/{sessionId}/finish")
    @Operation(summary = "Finish test and get result")
    public ResponseEntity<SessionResultResponse> finishSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.finishSession(sessionId, user.getId()));
    }

    /**
     * Pause session (user pressed "Да, хочу" in "Прервать тест?" modal).
     * Session is saved and can be resumed until the timer expires.
     */
    @PostMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause session (resumable until timer expires)")
    public ResponseEntity<ApiResponse> pauseSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        sessionService.pauseSession(sessionId, user.getId());
        return ResponseEntity.ok(new ApiResponse(true, "Session paused. You can resume later."));
    }

    /**
     * Get result of a completed session.
     */
    @GetMapping("/sessions/{sessionId}/result")
    @Operation(summary = "Get test result")
    public ResponseEntity<SessionResultResponse> getResult(
            @PathVariable Long sessionId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(sessionService.getResult(sessionId, user.getId()));
    }

    /**
     * AI analysis of selected wrong answers.
     */
    @PostMapping("/sessions/{sessionId}/analyze-errors")
    @Operation(summary = "AI analysis of wrong answers (Claude API)")
    public ResponseEntity<ErrorAnalysisResponse> analyzeErrors(
            @PathVariable Long sessionId,
            @RequestBody ErrorAnalysisRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        request.setSessionId(sessionId);
        return ResponseEntity.ok(sessionService.analyzeErrors(sessionId, user.getId(), request));
    }
}
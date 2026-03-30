package synamyk.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import synamyk.dto.MessageResponse;
import synamyk.entities.User;
import synamyk.exception.AppException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<MessageResponse> handleAppException(AppException ex, HttpServletRequest request) {
        String lang = resolveLang(request);
        String message = "KY".equalsIgnoreCase(lang) ? ex.getMessageKy() : ex.getMessageRu();
        return ResponseEntity.badRequest().body(new MessageResponse(false, message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new MessageResponse(false, ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String lang = resolveLang(request);
        String message = "KY".equalsIgnoreCase(lang)
                ? "Туура эмес номер же сырсөз."
                : "Неверный номер или пароль.";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(false, message));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabled(DisabledException ex, HttpServletRequest request) {
        String lang = resolveLang(request);
        String message = "KY".equalsIgnoreCase(lang)
                ? "Аккаунт өчүрүлгөн же номер тастыкталган жок."
                : "Аккаунт отключён или номер не подтверждён.";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(false, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    // ===== HELPERS =====

    private String resolveLang(HttpServletRequest request) {
        // 1. Authenticated user's saved preference
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getLanguage() != null ? user.getLanguage() : "RU";
        }
        // 2. Accept-Language request header (e.g. "ky", "ky-KG")
        String acceptLang = request.getHeader("Accept-Language");
        if (acceptLang != null && acceptLang.toLowerCase().startsWith("ky")) {
            return "KY";
        }
        // 3. ?lang= query param
        String lang = request.getParameter("lang");
        if ("KY".equalsIgnoreCase(lang)) return "KY";
        return "RU";
    }
}

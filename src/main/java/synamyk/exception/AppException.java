package synamyk.exception;

import lombok.Getter;

/**
 * Application-level exception carrying localized messages in both Russian and Kyrgyz.
 * The appropriate message is selected in {@code GlobalExceptionHandler} based on
 * the user's language preference.
 */
@Getter
public class AppException extends RuntimeException {

    private final String messageRu;
    private final String messageKy;

    public AppException(String messageRu, String messageKy) {
        super(messageRu);
        this.messageRu = messageRu;
        this.messageKy = messageKy;
    }
}

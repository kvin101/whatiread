package com.whatiread.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.whatiread.shared.exception.ConflictException;
import com.whatiread.shared.exception.ForbiddenException;
import com.whatiread.shared.exception.ResourceNotFoundException;
import com.whatiread.shared.exception.TooManyRequestsException;
import com.whatiread.shared.exception.UnauthorizedException;
import com.whatiread.shared.web.ProblemTypes;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private static final String BOOK_NOT_FOUND = "Book not found";
    private static final String TOKEN_EXPIRED = "Token expired";
    private static final String INVALID_RATING = "Invalid rating";
    private static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private static final String ACCESS_DENIED = "Access denied";
    private static final String MALFORMED_BODY = "Malformed request body";
    private static final String TARGET_TYPE_PARAM = "targetType";
    private static final String REQUEST_OBJECT = "request";
    private static final String BOOK_ID_PARAM = "bookId";
    private static final String FIELD_ERRORS_KEY = "fieldErrors";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void resourceNotFoundReturns404WithTypeUri() {
        ProblemDetail problem = handler.handleNotFound(new ResourceNotFoundException(BOOK_NOT_FOUND));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getType().toString()).isEqualTo(ProblemTypes.uriString(ProblemTypes.NOT_FOUND));
        assertThat(problem.getDetail()).isEqualTo(BOOK_NOT_FOUND);
    }

    @Test
    void conflictReturns409() {
        ProblemDetail problem = handler.handleConflict(new ConflictException("Already exists"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getType().toString()).isEqualTo(ProblemTypes.uriString(ProblemTypes.CONFLICT));
    }

    @Test
    void unauthorizedReturns401() {
        ProblemDetail problem = handler.handleUnauthorized(new UnauthorizedException(TOKEN_EXPIRED));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo(TOKEN_EXPIRED);
    }

    @Test
    void badCredentialsReturnsGenericMessage() {
        ProblemDetail problem = handler.handleUnauthorized(new BadCredentialsException("bad"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(problem.getDetail()).isEqualTo(INVALID_CREDENTIALS);
    }

    @Test
    void forbiddenReturns403() {
        ProblemDetail problem = handler.handleForbidden(new ForbiddenException("Not allowed"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void accessDeniedReturns403() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getDetail()).isEqualTo(ACCESS_DENIED);
    }

    @Test
    void illegalStateReturns500WithoutLeakingDetail() {
        ProblemDetail problem = handler.handleIllegalState(
                new IllegalStateException("Could not export library as JSON"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getType().toString()).isEqualTo(ProblemTypes.uriString(ProblemTypes.INTERNAL_ERROR));
        assertThat(problem.getDetail()).isEqualTo(UNEXPECTED_ERROR);
    }

    @Test
    void illegalArgumentReturns400() {
        ProblemDetail problem = handler.handleBadRequest(new IllegalArgumentException(INVALID_RATING));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo(INVALID_RATING);
    }

    @Test
    void malformedJsonReturns400() {
        ProblemDetail problem = handler.handleUnreadableMessage(
                new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class)));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo(MALFORMED_BODY);
    }

    @Test
    void missingParameterReturns400() {
        ProblemDetail problem = handler.handleMissingParameter(
                new MissingServletRequestParameterException(TARGET_TYPE_PARAM, "String"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains(TARGET_TYPE_PARAM);
    }

    @Test
    void typeMismatchReturns400() {
        ProblemDetail problem = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("not-a-uuid", java.util.UUID.class, BOOK_ID_PARAM, null, null));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).contains(BOOK_ID_PARAM);
    }

    @Test
    void methodArgumentNotValidReturnsFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), REQUEST_OBJECT);
        bindingResult.addError(new FieldError(REQUEST_OBJECT, "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getType().toString()).isEqualTo(ProblemTypes.uriString(ProblemTypes.VALIDATION));
        assertThat(problem.getProperties()).containsKey(FIELD_ERRORS_KEY);
    }

    @Test
    void constraintViolationReturns400() {
        ProblemDetail problem = handler.handleConstraintViolation(
                new ConstraintViolationException("size must be between 1 and 100", Set.of()));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getType().toString()).isEqualTo(ProblemTypes.uriString(ProblemTypes.VALIDATION));
    }

    @Test
    void tooManyRequestsReturns429() {
        ProblemDetail problem = handler.handleTooManyRequests(
                new TooManyRequestsException("Slow down"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingDetail() {
        ProblemDetail problem = handler.handleUnexpected(new RuntimeException("database connection lost"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo(UNEXPECTED_ERROR);
    }
}

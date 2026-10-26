package pl.zai.airquality.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemBody> handleApi(ApiException ex, HttpServletRequest request) {
        return problem(ex.getStatus(), ex.getTitle(), ex.getMessage(), request.getRequestURI(), ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemBody> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ProblemBody.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Request validation failed", request.getRequestURI(), errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemBody> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Malformed JSON or invalid field types", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemBody> handleUnsupported(HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "Request body must be application/json", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleNotAcceptable() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemBody> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "Method " + ex.getMethod() + " is not supported for this resource", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemBody> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid parameter: " + ex.getName(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ProblemBody> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        if ("X-API-Key".equalsIgnoreCase(ex.getHeaderName())) {
            return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Missing sensor API key", request.getRequestURI(), List.of());
        }
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", "Missing header: " + ex.getHeaderName(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ProblemBody> handleNotFound(HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", "Resource not found", request.getRequestURI(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemBody> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error", request.getRequestURI(), List.of());
    }

    private ProblemBody.FieldError toFieldError(FieldError error) {
        return new ProblemBody.FieldError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ProblemBody> problem(
            HttpStatus status,
            String title,
            String detail,
            String instance,
            List<ProblemBody.FieldError> errors
    ) {
        ProblemBody body = ProblemBody.of(status.value(), title, detail, instance, errors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}

package pl.zai.airquality.web;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final List<ProblemBody.FieldError> errors;

    public ApiException(HttpStatus status, String title, String detail) {
        this(status, title, detail, List.of());
    }

    public ApiException(HttpStatus status, String title, String detail, List<ProblemBody.FieldError> errors) {
        super(detail);
        this.status = status;
        this.title = title;
        this.errors = errors;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "Bad Request", detail);
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }

    public static ApiException forbidden(String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, "Forbidden", detail);
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, "Not Found", detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "Conflict", detail);
    }

    public static ApiException unprocessable(String detail) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable Entity", detail);
    }

    public static ApiException unprocessable(String detail, List<ProblemBody.FieldError> errors) {
        return new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable Entity", detail, errors);
    }

    public static ApiException unsupportedMediaType() {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", "Request body must be application/json");
    }

    public static ApiException notAcceptable() {
        return new ApiException(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", "This API only produces application/json");
    }
}

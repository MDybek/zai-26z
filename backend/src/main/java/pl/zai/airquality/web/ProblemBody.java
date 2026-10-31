package pl.zai.airquality.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ProblemBody(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {
    }

    public static ProblemBody of(int status, String title, String detail, String instance, List<FieldError> errors) {
        return new ProblemBody("about:blank", title, status, detail, instance, errors);
    }
}

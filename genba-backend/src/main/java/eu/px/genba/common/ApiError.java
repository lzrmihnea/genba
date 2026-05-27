package eu.px.genba.common;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * Standard error envelope returned by all REST endpoints.
 *
 * <p>{@link #messageKey} carries the i18n key so a client can localize
 * server-resolved messages itself; {@link #message} is the same key resolved
 * against the request locale for clients that want a ready-to-render string.
 *
 * @param messageKey i18n key, e.g. {@code auth.error.invalidCredentials}
 * @param message    localized message using the request {@code Accept-Language}
 * @param status     HTTP status code mirrored in the body for convenience
 * @param errorCode  short machine-readable code, e.g. {@code INVALID_CREDENTIALS}
 * @param timestamp  when the error was produced
 * @param path       request path that produced the error
 * @param fieldErrors per-field validation messages keyed by field name
 */
@Builder
public record ApiError(
        String messageKey,
        String message,
        int status,
        String errorCode,
        Instant timestamp,
        String path,
        Map<String, List<String>> fieldErrors) {
}

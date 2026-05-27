package eu.px.genba.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.px.genba.common.ApiError;
import eu.px.genba.i18n.MessageProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Returns a localized {@link ApiError} as JSON on 401 instead of Spring's
 * default empty body — keeps the auth-failure contract identical to the
 * domain-error envelope used elsewhere.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final MessageProvider messageProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String messageKey = "auth.error.unauthorized";
        ApiError error = ApiError.builder()
                .messageKey(messageKey)
                .message(messageProvider.getForRequest(request, messageKey))
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .errorCode("UNAUTHORIZED")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        objectMapper.writeValue(response.getWriter(), error);
    }
}

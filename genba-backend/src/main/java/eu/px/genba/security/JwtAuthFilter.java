package eu.px.genba.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves a Bearer access token into a Spring Security authentication.
 *
 * <p>Stateless: never touches the database. The JWT's signature is the
 * authority; if it verifies, the embedded {@code userId} + {@code email} are
 * trusted for the life of the (short-lived) access token. Refresh-time checks
 * against the user table happen in {@link eu.px.genba.auth.AuthService}.
 *
 * <p>Refresh tokens presented as Bearer credentials are rejected — they may
 * be exchanged only at {@code /api/auth/refresh}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws ServletException, IOException {

        String token = extractToken(request);
        if (token != null) {
            try {
                Claims claims = tokenProvider.parseClaims(token);
                JwtTokenType type = tokenProvider.extractType(claims);
                if (type != JwtTokenType.ACCESS) {
                    log.debug("Rejected non-access token presented on Authorization header");
                } else {
                    GenbaUserPrincipal principal = new GenbaUserPrincipal(
                            tokenProvider.extractUserId(claims),
                            tokenProvider.extractEmail(claims));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, List.of());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                // Leave context anonymous; downstream entry point returns 401.
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}

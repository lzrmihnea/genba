package eu.px.genba.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper for reading the authenticated user out of the Spring Security context.
 *
 * <p>Returns {@link Optional#empty()} when no JWT was supplied or the
 * authentication is anonymous — callers decide how to handle that.
 */
@Component
public class CurrentUser {

    public Optional<GenbaUserPrincipal> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof GenbaUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public Optional<UUID> getUserId() {
        return get().map(GenbaUserPrincipal::userId);
    }

    public UUID requireUserId() {
        return getUserId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user in context"));
    }
}

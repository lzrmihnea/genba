package eu.px.genba.security;

import java.security.Principal;
import java.util.UUID;

/**
 * Minimal authenticated-user view extracted from a JWT.
 *
 * <p>Stays stateless: every request reads {@code userId} + {@code email}
 * directly from validated JWT claims, no DB round-trip. The refresh endpoint
 * is the only place that revalidates the user against the database.
 */
public record GenbaUserPrincipal(UUID userId, String email) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}

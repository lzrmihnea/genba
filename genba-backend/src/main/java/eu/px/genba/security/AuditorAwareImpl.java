package eu.px.genba.security;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * Resolves the current user id for {@code @CreatedBy} / {@code @LastModifiedBy}
 * fields on JPA entities. Returns empty during anonymous / boot-time work
 * (seed runner, migrations) — entities that have non-null audit columns must
 * supply them explicitly in those flows.
 */
@Component
@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<UUID> {

    private final CurrentUser currentUser;

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return currentUser.getUserId();
    }
}

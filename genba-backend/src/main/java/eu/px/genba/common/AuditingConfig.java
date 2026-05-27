package eu.px.genba.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate} on JPA entities.
 *
 * <p>{@code @CreatedBy} / {@code @LastModifiedBy} are intentionally NOT wired
 * yet — they require a {@code AuditorAware} bean that resolves the current
 * user. That arrives in section 5 once the JWT auth filter populates the
 * Spring Security context. Until then, audit-user columns on tables are
 * populated explicitly by services that already know the current user.
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig {
}

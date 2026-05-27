package eu.px.genba.common;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate} on JPA entities,
 * plus {@code @CreatedBy} / {@code @LastModifiedBy} resolved from the JWT
 * authentication context via {@link eu.px.genba.security.AuditorAwareImpl}.
 *
 * <p>During anonymous flows (seed runner, migrations, healthchecks) the
 * AuditorAware returns empty and the audit-user columns stay NULL — entities
 * that need a populated value in those flows must supply it explicitly.
 *
 * <p>{@link DateTimeProvider} returns {@link OffsetDateTime}, matching the
 * {@code TIMESTAMP WITH TIME ZONE} columns and the entity field types.
 * Spring Data's default {@code CurrentDateTimeProvider} returns
 * {@code LocalDateTime}, which Spring's ConversionService cannot losslessly
 * widen to {@code OffsetDateTime}, so we provide our own.
 */
@Configuration
@EnableJpaAuditing(
        auditorAwareRef = "auditorAwareImpl",
        dateTimeProviderRef = "offsetDateTimeProvider")
public class AuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}

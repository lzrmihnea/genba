package eu.px.genba.user;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/**
 * Outbound user representation. Password hash deliberately omitted.
 */
@Builder
public record UserDto(
        UUID id,
        String email,
        String displayName,
        String preferredLocale,
        boolean active,
        OffsetDateTime createdAt) {
}

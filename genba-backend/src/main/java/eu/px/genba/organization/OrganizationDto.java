package eu.px.genba.organization;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrganizationDto(
        UUID id,
        String name,
        OffsetDateTime createdAt) {
}

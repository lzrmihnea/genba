package eu.px.genba.project;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ProjectDto(
        UUID id,
        UUID orgId,
        String name,
        String description,
        String address,
        String baseCurrency,
        ProjectStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}

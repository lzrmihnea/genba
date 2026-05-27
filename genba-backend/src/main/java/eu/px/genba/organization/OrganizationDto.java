package eu.px.genba.organization;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OrganizationDto(
        UUID id,
        String name,
        String countryCode,
        String currencyCode,
        String vatRegime,
        UUID permitWorkflowTemplateId,
        OffsetDateTime createdAt) {
}

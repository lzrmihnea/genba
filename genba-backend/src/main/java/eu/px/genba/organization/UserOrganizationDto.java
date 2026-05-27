package eu.px.genba.organization;

import java.util.UUID;
import lombok.Builder;

/**
 * Outbound membership representation — the slim view returned alongside auth
 * responses so the frontend can render the active-org switcher and locale
 * defaults without a follow-up call.
 */
@Builder
public record UserOrganizationDto(
        UUID organizationId,
        String organizationName,
        String countryCode,
        String currencyCode,
        String vatRegime,
        OrgRole orgRole) {
}

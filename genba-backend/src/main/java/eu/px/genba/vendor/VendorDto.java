package eu.px.genba.vendor;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record VendorDto(
        UUID id,
        UUID orgId,
        UUID projectId,
        String name,
        String contactName,
        String phone,
        String email,
        String vatId,
        BigDecimal defaultRetentionPct,
        String notes) {
}

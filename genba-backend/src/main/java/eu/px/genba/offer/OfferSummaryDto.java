package eu.px.genba.offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

/** Slim view returned by the LIST endpoint — no line array. */
@Builder
public record OfferSummaryDto(
        UUID id,
        UUID projectId,
        UUID vendorId,
        String label,
        LocalDate receivedAt,
        LocalDate validUntil,
        String currencyCode,
        BigDecimal totalAmountExclVat,
        BigDecimal totalAmountInclVat,
        OfferStatus status,
        OffsetDateTime createdAt) {
}

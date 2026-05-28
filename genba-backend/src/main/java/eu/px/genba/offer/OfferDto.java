package eu.px.genba.offer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

/** Detail view returned by GET /api/offers/{id} — includes the line array. */
@Builder
public record OfferDto(
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
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OfferLineDto> lines) {
}

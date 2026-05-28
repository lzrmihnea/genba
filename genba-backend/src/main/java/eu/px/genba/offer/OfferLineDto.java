package eu.px.genba.offer;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

@Builder
public record OfferLineDto(
        UUID id,
        UUID offerId,
        int lineOrder,
        String label,
        String description,
        BigDecimal qty,
        String unit,
        BigDecimal unitPrice,
        String currencyCode,
        BigDecimal vatRate,
        BigDecimal lineTotalExclVat,
        BigDecimal lineTotalInclVat,
        UUID wbsItemId,
        String normalizedKey,
        String notes) {
}

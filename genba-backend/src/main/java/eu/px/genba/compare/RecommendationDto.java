package eu.px.genba.compare;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;

/**
 * A line present in another offer of the project but missing from the current
 * one. {@code source} is MATCH_GROUP (high confidence) or LABEL (medium).
 * {@code similarCount} = how many other offers contain this scope, used to
 * float consensus items to the top.
 */
@Builder
public record RecommendationDto(
        UUID sourceLineId,
        UUID sourceOfferId,
        String sourceVendorName,
        String label,
        BigDecimal qty,
        String unit,
        BigDecimal unitPrice,
        String currencyCode,
        BigDecimal vatRate,
        UUID sourceMatchGroupId,
        String source,
        int similarCount) {
}

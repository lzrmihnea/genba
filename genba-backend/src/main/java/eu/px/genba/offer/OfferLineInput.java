package eu.px.genba.offer;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Shared shape for single + bulk line creation. {@code currencyCode} defaults
 * to the parent Offer's currency when null; {@code lineOrder} appends to the
 * end when null.
 */
public record OfferLineInput(
        @NotBlank @Size(max = 512) String label,
        String description,
        @NotNull @DecimalMin("0.0001") BigDecimal qty,
        @Size(max = 32) String unit,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @Pattern(regexp = "^[A-Z]{3}$") String currencyCode,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal vatRate,
        String notes,
        Integer lineOrder) {
}

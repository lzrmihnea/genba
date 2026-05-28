package eu.px.genba.offer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOfferRequest(
        @NotNull UUID projectId,
        @NotNull UUID vendorId,
        @Size(max = 255) String label,
        LocalDate receivedAt,
        LocalDate validUntil,
        @Pattern(regexp = "^[A-Z]{3}$") String currencyCode,
        String notes) {
}

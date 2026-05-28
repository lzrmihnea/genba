package eu.px.genba.offer;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CloneOfferRequest(
        UUID vendorId,
        @Size(max = 255) String label,
        LocalDate receivedAt) {
}

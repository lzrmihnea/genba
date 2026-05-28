package eu.px.genba.offer;

import jakarta.validation.constraints.NotNull;

public record ChangeOfferStatusRequest(@NotNull OfferStatus status) {
}

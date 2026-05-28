package eu.px.genba.offer;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Offer lifecycle. Transition rules:
 *
 * <pre>
 *   DRAFT     → RECEIVED
 *   RECEIVED  → ACCEPTED | REJECTED | EXPIRED
 *   ACCEPTED  → EXPIRED
 *   REJECTED  → DRAFT (allow correction)
 *   EXPIRED   → ∅ (terminal)
 * </pre>
 */
public enum OfferStatus {
    DRAFT,
    RECEIVED,
    ACCEPTED,
    REJECTED,
    EXPIRED;

    private static final Map<OfferStatus, Set<OfferStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(RECEIVED),
            RECEIVED, EnumSet.of(ACCEPTED, REJECTED, EXPIRED),
            ACCEPTED, EnumSet.of(EXPIRED),
            REJECTED, EnumSet.of(DRAFT),
            EXPIRED, EnumSet.noneOf(OfferStatus.class));

    public boolean canTransitionTo(OfferStatus target) {
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(OfferStatus.class)).contains(target);
    }
}

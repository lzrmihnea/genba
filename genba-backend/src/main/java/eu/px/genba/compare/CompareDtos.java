package eu.px.genba.compare;

import eu.px.genba.offer.OfferLineDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/** DTOs for the side-by-side comparison response. */
public final class CompareDtos {

    private CompareDtos() {
    }

    public enum RowKind {
        GROUP,
        UNMATCHED
    }

    /**
     * One comparison row. {@code cells} is keyed by offer id; a present key with
     * a null value means that offer is MISSING this row's scope. {@code warnings}
     * holds {@code CURRENCY_MISMATCH} / {@code UNIT_MISMATCH} flags.
     */
    @Builder
    public record CompareRow(
            String rowKey,
            RowKind kind,
            String label,
            Map<UUID, OfferLineDto> cells,
            List<String> warnings) {
    }

    @Builder
    public record CompareAggregate(
            BigDecimal totalExclVat,
            BigDecimal totalInclVat,
            int matchedCount,
            int missingCount) {
    }

    @Builder
    public record CompareResult(
            List<UUID> offerIds,
            List<CompareRow> rows,
            Map<UUID, CompareAggregate> perOfferAggregate) {
    }

    /** One auto-suggest proposal: ≥2 lines with identical normalized labels. */
    @Builder
    public record MatchSuggestion(
            String label,
            List<UUID> candidateLineIds) {
    }
}

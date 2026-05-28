package eu.px.genba.compare;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MatchGroupDto(
        UUID id,
        UUID projectId,
        String label,
        String notes,
        List<UUID> offerLineIds) {
}

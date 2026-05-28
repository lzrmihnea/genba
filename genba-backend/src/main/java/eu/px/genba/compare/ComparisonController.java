package eu.px.genba.compare;

import eu.px.genba.compare.CompareDtos.CompareResult;
import eu.px.genba.compare.CompareDtos.MatchSuggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bid comparison", description = "Side-by-side offer comparison + exact-label auto-suggest")
public class ComparisonController {

    private final ComparisonService service;

    @GetMapping("/api/projects/{projectId}/compare")
    @Operation(summary = "Row-major comparison of offers; rows are MatchGroups or unmatched lines, columns are offers")
    public ResponseEntity<CompareResult> compare(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID projectId,
            @RequestParam(value = "offer_ids", required = false) List<UUID> offerIds) {
        return ResponseEntity.ok(service.compare(orgId, projectId, offerIds));
    }

    @PostMapping("/api/projects/{projectId}/compare/suggest-matches")
    @Operation(summary = "Propose MatchGroups from exact (case-insensitive) label equality; not persisted")
    public ResponseEntity<List<MatchSuggestion>> suggest(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID projectId,
            @RequestParam(value = "offer_ids", required = false) List<UUID> offerIds) {
        return ResponseEntity.ok(service.suggestMatches(orgId, projectId, offerIds));
    }
}

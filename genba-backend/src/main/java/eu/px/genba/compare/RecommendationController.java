package eu.px.genba.compare;

import eu.px.genba.offer.Offer;
import eu.px.genba.offer.OfferLineDto;
import eu.px.genba.offer.OfferLineInput;
import eu.px.genba.offer.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Offer recommendations", description = "While-entering: lines from other offers missing in the current one")
public class RecommendationController {

    private final RecommendationService service;
    private final OfferService offerService;

    @GetMapping("/api/projects/{projectId}/offers/{offerId}/missing-from-others")
    @Operation(summary = "Lines in other offers of this project not yet represented in the current offer")
    public ResponseEntity<List<RecommendationDto>> missingFromOthers(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID projectId,
            @PathVariable UUID offerId) {
        return ResponseEntity.ok(service.missingFromOthers(orgId, projectId, offerId));
    }

    @PostMapping("/api/offers/{offerId}/lines/from-recommendation")
    @Operation(summary = "Copy a recommended source line into the current offer + link them via a MatchGroup")
    public ResponseEntity<OfferLineDto> addFromRecommendation(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID offerId,
            @RequestBody AddFromRecommendationRequest request) {
        Offer current = offerService.requireActive(orgId, offerId);
        OfferLineDto dto = service.addFromRecommendation(
                orgId, current.getProjectId(), offerId, request.sourceLineId(), request.overrides());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/api/offers/{offerId}/recommendations/{sourceLineId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID offerId,
            @PathVariable UUID sourceLineId) {
        Offer current = offerService.requireActive(orgId, offerId);
        service.dismiss(orgId, current.getProjectId(), offerId, sourceLineId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/offers/{offerId}/recommendations/{sourceLineId}/dismiss")
    public ResponseEntity<Void> undismiss(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID offerId,
            @PathVariable UUID sourceLineId) {
        service.undismiss(orgId, offerId, sourceLineId);
        return ResponseEntity.noContent().build();
    }

    public record AddFromRecommendationRequest(UUID sourceLineId, OfferLineInput overrides) {
    }
}

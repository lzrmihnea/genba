package eu.px.genba.offer;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lines")
@RequiredArgsConstructor
@Tag(name = "Offer lines", description = "Direct line-level mutations triggering parent-offer recompute")
public class OfferLineController {

    private final OfferService offerService;
    private final OfferLineService lineService;
    private final OfferMapper mapper;

    @PutMapping("/{lineId}")
    public ResponseEntity<OfferLineDto> update(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID lineId,
            @RequestParam("offer_id") UUID offerId,
            @Valid @RequestBody OfferLineInput input) {
        Offer offer = offerService.requireActive(orgId, offerId);
        OfferLine updated = lineService.updateLine(lineId, offer.getId(), input);
        offerService.recomputeTotals(offer);
        return ResponseEntity.ok(mapper.toLineDto(updated));
    }

    @PatchMapping("/{lineId}/order")
    public ResponseEntity<Void> reorder(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID lineId,
            @RequestParam("offer_id") UUID offerId,
            @RequestParam("line_order") @NotNull Integer newOrder) {
        Offer offer = offerService.requireActive(orgId, offerId);
        lineService.updateOrder(lineId, offer.getId(), newOrder);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{lineId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID lineId,
            @RequestParam("offer_id") UUID offerId) {
        Offer offer = offerService.requireActive(orgId, offerId);
        lineService.deleteLine(lineId, offer.getId());
        offerService.recomputeTotals(offer);
        return ResponseEntity.noContent().build();
    }
}

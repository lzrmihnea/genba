package eu.px.genba.offer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
@Tag(name = "Offers", description = "Structured bids from Vendors, with line items and status workflow")
public class OfferController {

    private final OfferService service;
    private final OfferLineService lineService;
    private final OfferMapper mapper;

    @GetMapping
    public ResponseEntity<List<OfferSummaryDto>> list(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @RequestParam("project_id") UUID projectId,
            @RequestParam(value = "status", required = false) OfferStatus status) {
        return ResponseEntity.ok(service.list(orgId, projectId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OfferDto> get(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.get(orgId, id));
    }

    @PostMapping
    public ResponseEntity<OfferDto> create(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @Valid @RequestBody CreateOfferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(orgId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OfferDto> update(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfferRequest request) {
        return ResponseEntity.ok(service.update(orgId, id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition the offer status; rejects illegal transitions with 400 ILLEGAL_STATUS_TRANSITION")
    public ResponseEntity<OfferDto> changeStatus(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody ChangeOfferStatusRequest request) {
        return ResponseEntity.ok(service.changeStatus(orgId, id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id) {
        service.softDelete(orgId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    @Operation(summary = "Clone an offer into a new DRAFT; copies lines (fresh ids), excludes attachments")
    public ResponseEntity<OfferDto> clone(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CloneOfferRequest request) {
        CloneOfferRequest payload = request != null ? request : new CloneOfferRequest(null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.clone(orgId, id, payload));
    }

    @PostMapping("/{id}/lines")
    public ResponseEntity<OfferLineDto> addLine(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody OfferLineInput input) {
        Offer offer = service.requireActive(orgId, id);
        OfferLine saved = lineService.addLine(offer, input);
        service.recomputeTotals(offer);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toLineDto(saved));
    }

    @PostMapping("/{id}/lines/bulk")
    @Operation(summary = "Bulk-create lines in one transaction (e.g. from a TSV / CSV paste)")
    public ResponseEntity<List<OfferLineDto>> addBulk(
            @RequestHeader("X-Genba-Org-Id") UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody List<OfferLineInput> inputs) {
        Offer offer = service.requireActive(orgId, id);
        List<OfferLine> saved = lineService.addBulk(offer, inputs);
        service.recomputeTotals(offer);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toLineDtos(saved));
    }
}

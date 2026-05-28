package eu.px.genba.offer;

import eu.px.genba.common.exception.GenbaException;
import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.vendor.Vendor;
import eu.px.genba.vendor.VendorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferLineRepository lineRepository;
    private final OfferLineService offerLineService;
    private final OfferMapper mapper;
    private final VendorService vendorService;

    @Transactional(readOnly = true)
    public List<OfferSummaryDto> list(UUID orgId, UUID projectId, OfferStatus status) {
        return offerRepository.findActiveForProject(orgId, projectId, status).stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public OfferDto get(UUID orgId, UUID offerId) {
        Offer offer = offerRepository.findActiveByIdInOrg(offerId, orgId)
                .orElseThrow(NotFoundException::new);
        List<OfferLine> lines = lineRepository.findByOffer(offerId);
        return mapper.toDto(offer, lines);
    }

    @Transactional
    public OfferDto create(UUID orgId, CreateOfferRequest request) {
        Vendor vendor = vendorService.requireActive(orgId, request.vendorId());
        String currency = request.currencyCode() != null ? request.currencyCode() : "RON";
        Offer offer = Offer.builder()
                .orgId(orgId)
                .projectId(request.projectId())
                .vendorId(vendor.getId())
                .label(request.label())
                .receivedAt(request.receivedAt() != null ? request.receivedAt() : LocalDate.now())
                .validUntil(request.validUntil())
                .currencyCode(currency)
                .status(OfferStatus.DRAFT)
                .notes(request.notes())
                .build();
        Offer saved = offerRepository.save(offer);
        return mapper.toDto(saved, List.of());
    }

    @Transactional
    public OfferDto update(UUID orgId, UUID offerId, UpdateOfferRequest request) {
        Offer offer = requireActive(orgId, offerId);
        if (request.vendorId() != null) {
            vendorService.requireActive(orgId, request.vendorId());
            offer.setVendorId(request.vendorId());
        }
        if (request.label() != null) offer.setLabel(request.label());
        if (request.receivedAt() != null) offer.setReceivedAt(request.receivedAt());
        if (request.validUntil() != null) offer.setValidUntil(request.validUntil());
        if (request.currencyCode() != null) offer.setCurrencyCode(request.currencyCode());
        if (request.notes() != null) offer.setNotes(request.notes());
        Offer saved = offerRepository.save(offer);
        return mapper.toDto(saved, lineRepository.findByOffer(saved.getId()));
    }

    @Transactional
    public OfferDto changeStatus(UUID orgId, UUID offerId, OfferStatus target) {
        Offer offer = requireActive(orgId, offerId);
        if (!offer.getStatus().canTransitionTo(target)) {
            throw new IllegalStatusTransitionException();
        }
        offer.setStatus(target);
        Offer saved = offerRepository.save(offer);
        return mapper.toDto(saved, lineRepository.findByOffer(saved.getId()));
    }

    @Transactional
    public void softDelete(UUID orgId, UUID offerId) {
        Offer offer = requireActive(orgId, offerId);
        offer.setDeletedAt(OffsetDateTime.now());
        offerRepository.save(offer);
    }

    @Transactional
    public OfferDto clone(UUID orgId, UUID sourceId, CloneOfferRequest request) {
        Offer source = requireActive(orgId, sourceId);
        UUID vendorId = request.vendorId() != null ? request.vendorId() : source.getVendorId();
        if (request.vendorId() != null) {
            vendorService.requireActive(orgId, request.vendorId());
        }
        String label = request.label() != null
                ? request.label()
                : (source.getLabel() != null ? source.getLabel() + " (copy)" : null);
        Offer clone = Offer.builder()
                .orgId(orgId)
                .projectId(source.getProjectId())
                .vendorId(vendorId)
                .label(label)
                .receivedAt(request.receivedAt() != null ? request.receivedAt() : LocalDate.now())
                .currencyCode(source.getCurrencyCode())
                .status(OfferStatus.DRAFT)
                .notes(source.getNotes())
                .build();
        Offer savedClone = offerRepository.save(clone);

        // Copy lines (fresh ids, wbs/normalized_key reset).
        List<OfferLine> sourceLines = lineRepository.findByOffer(source.getId());
        int order = 0;
        for (OfferLine src : sourceLines) {
            OfferLine copy = OfferLine.builder()
                    .offerId(savedClone.getId())
                    .lineOrder(++order)
                    .label(src.getLabel())
                    .description(src.getDescription())
                    .qty(src.getQty())
                    .unit(src.getUnit())
                    .unitPrice(src.getUnitPrice())
                    .currencyCode(src.getCurrencyCode())
                    .vatRate(src.getVatRate())
                    .lineTotalExclVat(src.getLineTotalExclVat())
                    .lineTotalInclVat(src.getLineTotalInclVat())
                    .notes(src.getNotes())
                    .build();
            lineRepository.save(copy);
        }
        recomputeTotals(savedClone);
        return mapper.toDto(savedClone, lineRepository.findByOffer(savedClone.getId()));
    }

    /**
     * Public so line endpoints can trigger recompute after mutations. Sums in
     * Java rather than via a JPQL aggregate — handful of lines per offer makes
     * the in-memory pass trivially cheap and avoids the multi-value-projection
     * cliff in Spring Data JPQL.
     */
    @Transactional
    public void recomputeTotals(Offer offer) {
        List<OfferLine> lines = lineRepository.findByOffer(offer.getId());
        BigDecimal excl = lines.stream()
                .map(OfferLine::getLineTotalExclVat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal incl = lines.stream()
                .map(OfferLine::getLineTotalInclVat)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        offer.setTotalAmountExclVat(excl);
        offer.setTotalAmountInclVat(incl);
        offerRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public Offer requireActive(UUID orgId, UUID offerId) {
        return offerRepository.findActiveByIdInOrg(offerId, orgId)
                .orElseThrow(NotFoundException::new);
    }

    public static final class IllegalStatusTransitionException extends GenbaException {
        public IllegalStatusTransitionException() {
            super("offer.error.illegalStatusTransition", "ILLEGAL_STATUS_TRANSITION");
        }
    }
}

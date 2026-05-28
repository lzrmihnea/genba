package eu.px.genba.offer;

import eu.px.genba.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OfferLineService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    private final OfferLineRepository lineRepository;

    @Transactional
    public OfferLine addLine(Offer offer, OfferLineInput input) {
        OfferLine line = build(offer, input, lineRepository.maxLineOrder(offer.getId()) + 1);
        recomputeLineTotals(line);
        return lineRepository.save(line);
    }

    @Transactional
    public List<OfferLine> addBulk(Offer offer, List<OfferLineInput> inputs) {
        AtomicInteger next = new AtomicInteger(lineRepository.maxLineOrder(offer.getId()));
        List<OfferLine> built = inputs.stream()
                .map(input -> {
                    OfferLine line = build(offer, input, next.incrementAndGet());
                    recomputeLineTotals(line);
                    return line;
                })
                .toList();
        return lineRepository.saveAll(built);
    }

    @Transactional
    public OfferLine updateLine(UUID lineId, UUID offerId, OfferLineInput input) {
        OfferLine line = lineRepository.findByIdAndOfferId(lineId, offerId)
                .orElseThrow(NotFoundException::new);
        line.setLabel(input.label());
        line.setDescription(input.description());
        line.setQty(input.qty());
        if (input.unit() != null) line.setUnit(input.unit());
        line.setUnitPrice(input.unitPrice());
        if (input.currencyCode() != null) line.setCurrencyCode(input.currencyCode());
        if (input.vatRate() != null) line.setVatRate(input.vatRate());
        if (input.notes() != null) line.setNotes(input.notes());
        if (input.lineOrder() != null) line.setLineOrder(input.lineOrder());
        recomputeLineTotals(line);
        return lineRepository.save(line);
    }

    @Transactional
    public void deleteLine(UUID lineId, UUID offerId) {
        OfferLine line = lineRepository.findByIdAndOfferId(lineId, offerId)
                .orElseThrow(NotFoundException::new);
        lineRepository.delete(line);
    }

    @Transactional
    public void updateOrder(UUID lineId, UUID offerId, int newOrder) {
        OfferLine line = lineRepository.findByIdAndOfferId(lineId, offerId)
                .orElseThrow(NotFoundException::new);
        line.setLineOrder(newOrder);
        lineRepository.save(line);
    }

    @Transactional(readOnly = true)
    public List<OfferLine> listForOffer(UUID offerId) {
        return lineRepository.findByOffer(offerId);
    }

    private OfferLine build(Offer offer, OfferLineInput input, int lineOrder) {
        return OfferLine.builder()
                .offerId(offer.getId())
                .lineOrder(input.lineOrder() != null ? input.lineOrder() : lineOrder)
                .label(input.label())
                .description(input.description())
                .qty(input.qty())
                .unit(input.unit() != null ? input.unit() : "buc")
                .unitPrice(input.unitPrice())
                .currencyCode(input.currencyCode() != null ? input.currencyCode() : offer.getCurrencyCode())
                .vatRate(input.vatRate() != null ? input.vatRate() : new BigDecimal("19.00"))
                .notes(input.notes())
                .build();
    }

    /** {@code excl = qty × unit_price}, rounded; {@code incl = excl × (1 + vat/100)}, rounded. */
    static void recomputeLineTotals(OfferLine line) {
        BigDecimal excl = line.getQty()
                .multiply(line.getUnitPrice())
                .setScale(MONEY_SCALE, MONEY_ROUNDING);
        BigDecimal multiplier = BigDecimal.ONE.add(
                line.getVatRate().divide(HUNDRED, 6, MONEY_ROUNDING));
        BigDecimal incl = excl.multiply(multiplier).setScale(MONEY_SCALE, MONEY_ROUNDING);
        line.setLineTotalExclVat(excl);
        line.setLineTotalInclVat(incl);
    }
}

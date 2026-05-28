package eu.px.genba.compare;

import eu.px.genba.offer.Offer;
import eu.px.genba.offer.OfferLine;
import eu.px.genba.offer.OfferLineDto;
import eu.px.genba.offer.OfferLineInput;
import eu.px.genba.offer.OfferLineRepository;
import eu.px.genba.offer.OfferLineService;
import eu.px.genba.offer.OfferMapper;
import eu.px.genba.offer.OfferRepository;
import eu.px.genba.offer.OfferService;
import eu.px.genba.offer.OfferStatus;
import eu.px.genba.security.CurrentUser;
import eu.px.genba.vendor.VendorRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final List<OfferStatus> COMPARABLE =
            List.of(OfferStatus.DRAFT, OfferStatus.RECEIVED, OfferStatus.ACCEPTED);

    private final OfferRepository offerRepository;
    private final OfferLineRepository offerLineRepository;
    private final OfferLineMatchGroupRepository groupRepository;
    private final MatchGroupLineRepository lineLinkRepository;
    private final DismissedRecommendationRepository dismissedRepository;
    private final VendorRepository vendorRepository;
    private final OfferService offerService;
    private final OfferLineService offerLineService;
    private final MatchGroupService matchGroupService;
    private final OfferMapper offerMapper;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<RecommendationDto> missingFromOthers(UUID orgId, UUID projectId, UUID currentOfferId) {
        Offer current = offerService.requireActive(orgId, currentOfferId);

        List<OfferLine> currentLines = offerLineRepository.findByOffer(currentOfferId);
        Set<String> currentLabels = currentLines.stream()
                .map(l -> ComparisonService.normalize(l.getLabel()))
                .collect(Collectors.toSet());

        List<Offer> otherOffers = offerRepository.findComparable(orgId, projectId, COMPARABLE).stream()
                .filter(o -> !o.getId().equals(currentOfferId))
                .toList();
        if (otherOffers.isEmpty()) {
            return List.of();
        }
        Map<UUID, String> vendorNameByOffer = otherOffers.stream().collect(Collectors.toMap(
                Offer::getId,
                o -> vendorRepository.findActiveByIdInOrg(o.getVendorId(), orgId)
                        .map(v -> v.getName()).orElse("?")));

        List<UUID> otherOfferIds = otherOffers.stream().map(Offer::getId).toList();
        List<OfferLine> otherLines = offerLineRepository.findByOfferIds(otherOfferIds);

        // group memberships across the project
        Map<UUID, UUID> groupIdByLineId = lineLinkRepository.findAllForProject(projectId).stream()
                .collect(Collectors.toMap(MatchGroupLine::getOfferLineId, MatchGroupLine::getMatchGroupId, (a, b) -> a));
        Set<UUID> currentGroupIds = currentLines.stream()
                .map(l -> groupIdByLineId.get(l.getId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> dismissed = dismissedRepository.findByCurrentOfferId(currentOfferId).stream()
                .map(DismissedRecommendation::getSourceLineId)
                .collect(Collectors.toSet());

        // candidate lines: in a group not represented in current, OR label not present in current
        record Candidate(OfferLine line, String source) {}
        List<Candidate> candidates = new ArrayList<>();
        for (OfferLine line : otherLines) {
            if (dismissed.contains(line.getId())) {
                continue;
            }
            UUID groupId = groupIdByLineId.get(line.getId());
            if (groupId != null && !currentGroupIds.contains(groupId)) {
                candidates.add(new Candidate(line, "MATCH_GROUP"));
            } else if (groupId == null && !currentLabels.contains(ComparisonService.normalize(line.getLabel()))) {
                candidates.add(new Candidate(line, "LABEL"));
            }
        }

        // dedupe by normalized label, keep representative + count distinct offers
        Map<String, List<Candidate>> byLabel = candidates.stream()
                .collect(Collectors.groupingBy(c -> ComparisonService.normalize(c.line().getLabel()),
                        LinkedHashMap::new, Collectors.toList()));

        List<RecommendationDto> recs = new ArrayList<>();
        for (List<Candidate> bucket : byLabel.values()) {
            Candidate rep = bucket.get(0);
            int similar = (int) bucket.stream().map(c -> c.line().getOfferId()).distinct().count();
            OfferLine line = rep.line();
            recs.add(RecommendationDto.builder()
                    .sourceLineId(line.getId())
                    .sourceOfferId(line.getOfferId())
                    .sourceVendorName(vendorNameByOffer.getOrDefault(line.getOfferId(), "?"))
                    .label(line.getLabel())
                    .qty(line.getQty())
                    .unit(line.getUnit())
                    .unitPrice(line.getUnitPrice())
                    .currencyCode(line.getCurrencyCode())
                    .vatRate(line.getVatRate())
                    .sourceMatchGroupId(groupIdByLineId.get(line.getId()))
                    .source(rep.source())
                    .similarCount(similar)
                    .build());
        }
        recs.sort(Comparator.comparingInt(RecommendationDto::similarCount).reversed());
        return recs;
    }

    @Transactional
    public OfferLineDto addFromRecommendation(
            UUID orgId, UUID projectId, UUID currentOfferId, UUID sourceLineId, OfferLineInput overrides) {
        Offer current = offerService.requireActive(orgId, currentOfferId);
        OfferLine source = offerLineRepository.findById(sourceLineId)
                .orElseThrow(eu.px.genba.common.exception.NotFoundException::new);

        OfferLineInput input = overrides != null ? overrides : new OfferLineInput(
                source.getLabel(), source.getDescription(), source.getQty(), source.getUnit(),
                source.getUnitPrice(), source.getCurrencyCode(), source.getVatRate(), source.getNotes(), null);
        OfferLine newLine = offerLineService.addLine(current, input);
        offerService.recomputeTotals(current);

        // link new line to the source via match group (create or extend)
        lineLinkRepository.findByOfferLineId(sourceLineId).ifPresentOrElse(
                existing -> matchGroupService.addLine(orgId, projectId, existing.getMatchGroupId(), newLine.getId()),
                () -> matchGroupService.create(orgId, projectId,
                        new CreateMatchGroupRequest(List.of(sourceLineId, newLine.getId()), source.getLabel())));

        return offerMapper.toLineDto(newLine);
    }

    @Transactional
    public void dismiss(UUID orgId, UUID projectId, UUID currentOfferId, UUID sourceLineId) {
        offerService.requireActive(orgId, currentOfferId); // org-scope guard
        if (dismissedRepository.existsByCurrentOfferIdAndSourceLineId(currentOfferId, sourceLineId)) {
            return;
        }
        dismissedRepository.save(DismissedRecommendation.builder()
                .projectId(projectId)
                .currentOfferId(currentOfferId)
                .sourceLineId(sourceLineId)
                .build());
    }

    @Transactional
    public void undismiss(UUID orgId, UUID currentOfferId, UUID sourceLineId) {
        offerService.requireActive(orgId, currentOfferId);
        dismissedRepository.deleteByCurrentOfferIdAndSourceLineId(currentOfferId, sourceLineId);
    }
}

package eu.px.genba.compare;

import eu.px.genba.compare.CompareDtos.CompareAggregate;
import eu.px.genba.compare.CompareDtos.CompareResult;
import eu.px.genba.compare.CompareDtos.CompareRow;
import eu.px.genba.compare.CompareDtos.MatchSuggestion;
import eu.px.genba.compare.CompareDtos.RowKind;
import eu.px.genba.offer.Offer;
import eu.px.genba.offer.OfferLine;
import eu.px.genba.offer.OfferLineDto;
import eu.px.genba.offer.OfferMapper;
import eu.px.genba.offer.OfferRepository;
import eu.px.genba.offer.OfferLineRepository;
import eu.px.genba.offer.OfferStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ComparisonService {

    private static final List<OfferStatus> COMPARABLE =
            List.of(OfferStatus.DRAFT, OfferStatus.RECEIVED, OfferStatus.ACCEPTED);

    private final OfferRepository offerRepository;
    private final OfferLineRepository offerLineRepository;
    private final OfferLineMatchGroupRepository groupRepository;
    private final MatchGroupLineRepository lineLinkRepository;
    private final OfferMapper offerMapper;

    @Transactional(readOnly = true)
    public CompareResult compare(UUID orgId, UUID projectId, List<UUID> offerIds) {
        List<Offer> offers = (offerIds != null && !offerIds.isEmpty())
                ? offerRepository.findSelected(orgId, projectId, offerIds)
                : offerRepository.findComparable(orgId, projectId, COMPARABLE);

        List<UUID> selectedOfferIds = offers.stream().map(Offer::getId).toList();
        if (selectedOfferIds.isEmpty()) {
            return CompareResult.builder()
                    .offerIds(List.of()).rows(List.of()).perOfferAggregate(Map.of()).build();
        }

        List<OfferLine> lines = offerLineRepository.findByOfferIds(selectedOfferIds);
        Map<UUID, OfferLine> lineById = lines.stream()
                .collect(Collectors.toMap(OfferLine::getId, l -> l));

        // group memberships across the whole project, restricted to our selected lines
        Map<UUID, UUID> groupIdByLineId = new LinkedHashMap<>();
        Map<UUID, List<UUID>> lineIdsByGroup = new LinkedHashMap<>();
        for (MatchGroupLine link : lineLinkRepository.findAllForProject(projectId)) {
            if (!lineById.containsKey(link.getOfferLineId())) {
                continue; // line belongs to an offer not in this comparison
            }
            groupIdByLineId.put(link.getOfferLineId(), link.getMatchGroupId());
            lineIdsByGroup.computeIfAbsent(link.getMatchGroupId(), k -> new ArrayList<>())
                    .add(link.getOfferLineId());
        }

        List<OfferLineMatchGroup> groups = groupRepository.findByProjectId(projectId).stream()
                .filter(g -> lineIdsByGroup.containsKey(g.getId()))
                .sorted(Comparator.comparing(OfferLineMatchGroup::getCreatedAt).reversed())
                .toList();

        List<CompareRow> rows = new ArrayList<>();

        // GROUP rows
        for (OfferLineMatchGroup group : groups) {
            List<OfferLine> members = lineIdsByGroup.get(group.getId()).stream()
                    .map(lineById::get)
                    .toList();
            Map<UUID, OfferLineDto> cells = new LinkedHashMap<>();
            for (UUID offerId : selectedOfferIds) {
                Optional<OfferLine> cell = members.stream()
                        .filter(l -> l.getOfferId().equals(offerId))
                        .findFirst();
                cells.put(offerId, cell.map(offerMapper::toLineDto).orElse(null));
            }
            rows.add(CompareRow.builder()
                    .rowKey("group:" + group.getId())
                    .kind(RowKind.GROUP)
                    .label(group.getLabel())
                    .cells(cells)
                    .warnings(warningsFor(members))
                    .build());
        }

        // UNMATCHED rows: lines not in any group, in offer/line order
        for (OfferLine line : lines) {
            if (groupIdByLineId.containsKey(line.getId())) {
                continue;
            }
            Map<UUID, OfferLineDto> cells = new LinkedHashMap<>();
            for (UUID offerId : selectedOfferIds) {
                cells.put(offerId, offerId.equals(line.getOfferId()) ? offerMapper.toLineDto(line) : null);
            }
            rows.add(CompareRow.builder()
                    .rowKey("line:" + line.getId())
                    .kind(RowKind.UNMATCHED)
                    .label(line.getLabel())
                    .cells(cells)
                    .warnings(List.of())
                    .build());
        }

        // per-offer aggregates
        Map<UUID, CompareAggregate> aggregates = new LinkedHashMap<>();
        for (Offer offer : offers) {
            List<OfferLine> offerLines = lines.stream()
                    .filter(l -> l.getOfferId().equals(offer.getId()))
                    .toList();
            BigDecimal excl = offerLines.stream()
                    .map(OfferLine::getLineTotalExclVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal incl = offerLines.stream()
                    .map(OfferLine::getLineTotalInclVat).reduce(BigDecimal.ZERO, BigDecimal::add);
            int matched = (int) rows.stream().filter(r -> r.cells().get(offer.getId()) != null).count();
            int missing = rows.size() - matched;
            aggregates.put(offer.getId(), CompareAggregate.builder()
                    .totalExclVat(excl).totalInclVat(incl)
                    .matchedCount(matched).missingCount(missing)
                    .build());
        }

        return CompareResult.builder()
                .offerIds(selectedOfferIds)
                .rows(rows)
                .perOfferAggregate(aggregates)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestion> suggestMatches(UUID orgId, UUID projectId, List<UUID> offerIds) {
        List<Offer> offers = (offerIds != null && !offerIds.isEmpty())
                ? offerRepository.findSelected(orgId, projectId, offerIds)
                : offerRepository.findComparable(orgId, projectId, COMPARABLE);
        List<UUID> selectedOfferIds = offers.stream().map(Offer::getId).toList();
        if (selectedOfferIds.isEmpty()) {
            return List.of();
        }

        List<OfferLine> lines = offerLineRepository.findByOfferIds(selectedOfferIds);
        java.util.Set<UUID> grouped = lineLinkRepository.findAllForProject(projectId).stream()
                .map(MatchGroupLine::getOfferLineId)
                .collect(Collectors.toSet());

        Map<String, List<OfferLine>> byLabel = lines.stream()
                .filter(l -> !grouped.contains(l.getId()))
                .collect(Collectors.groupingBy(l -> normalize(l.getLabel()), LinkedHashMap::new, Collectors.toList()));

        List<MatchSuggestion> suggestions = new ArrayList<>();
        for (List<OfferLine> bucket : byLabel.values()) {
            long distinctOffers = bucket.stream().map(OfferLine::getOfferId).distinct().count();
            if (bucket.size() >= 2 && distinctOffers >= 2) {
                suggestions.add(MatchSuggestion.builder()
                        .label(bucket.get(0).getLabel())
                        .candidateLineIds(bucket.stream().map(OfferLine::getId).toList())
                        .build());
            }
        }
        return suggestions;
    }

    private static List<String> warningsFor(List<OfferLine> members) {
        List<String> warnings = new ArrayList<>();
        long currencies = members.stream().map(OfferLine::getCurrencyCode).distinct().count();
        long units = members.stream().map(OfferLine::getUnit).distinct().count();
        if (currencies > 1) warnings.add("CURRENCY_MISMATCH");
        if (units > 1) warnings.add("UNIT_MISMATCH");
        return warnings;
    }

    static String normalize(String label) {
        return label == null ? "" : label.trim().toLowerCase();
    }
}

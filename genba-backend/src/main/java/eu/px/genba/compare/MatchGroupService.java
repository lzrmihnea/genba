package eu.px.genba.compare;

import eu.px.genba.common.exception.NotFoundException;
import eu.px.genba.offer.Offer;
import eu.px.genba.offer.OfferLine;
import eu.px.genba.offer.OfferLineRepository;
import eu.px.genba.offer.OfferRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MatchGroupService {

    private final OfferLineMatchGroupRepository groupRepository;
    private final MatchGroupLineRepository lineLinkRepository;
    private final OfferLineRepository offerLineRepository;
    private final OfferRepository offerRepository;

    @Transactional
    public MatchGroupDto create(UUID orgId, UUID projectId, CreateMatchGroupRequest request) {
        List<UUID> lineIds = request.lines();
        if (lineIds == null || lineIds.size() < 2) {
            throw new MatchGroupExceptions.TooFewLines();
        }
        String label = request.label();
        for (UUID lineId : lineIds) {
            OfferLine line = requireLineInProject(orgId, projectId, lineId);
            if (label == null) {
                label = line.getLabel();
            }
            if (lineLinkRepository.findByOfferLineId(lineId).isPresent()) {
                throw new MatchGroupExceptions.LineAlreadyGrouped();
            }
        }

        OfferLineMatchGroup group = groupRepository.save(OfferLineMatchGroup.builder()
                .projectId(projectId)
                .label(label != null ? label : "Group")
                .build());
        for (UUID lineId : lineIds) {
            lineLinkRepository.save(MatchGroupLine.builder()
                    .matchGroupId(group.getId())
                    .offerLineId(lineId)
                    .build());
        }
        return toDto(group);
    }

    @Transactional
    public MatchGroupDto addLine(UUID orgId, UUID projectId, UUID groupId, UUID lineId) {
        OfferLineMatchGroup group = requireGroup(projectId, groupId);
        requireLineInProject(orgId, projectId, lineId);
        if (lineLinkRepository.findByOfferLineId(lineId).isPresent()) {
            throw new MatchGroupExceptions.LineAlreadyGrouped();
        }
        lineLinkRepository.save(MatchGroupLine.builder()
                .matchGroupId(group.getId())
                .offerLineId(lineId)
                .build());
        return toDto(group);
    }

    /** Removing a line that leaves the group with fewer than two members dissolves it. */
    @Transactional
    public void removeLine(UUID projectId, UUID groupId, UUID lineId) {
        OfferLineMatchGroup group = requireGroup(projectId, groupId);
        lineLinkRepository.findByOfferLineId(lineId)
                .filter(l -> l.getMatchGroupId().equals(group.getId()))
                .orElseThrow(NotFoundException::new);
        lineLinkRepository.deleteByOfferLineId(lineId);
        if (lineLinkRepository.countByMatchGroupId(group.getId()) < 2) {
            dissolve(projectId, group.getId());
        }
    }

    @Transactional
    public MatchGroupDto update(UUID projectId, UUID groupId, UpdateMatchGroupRequest request) {
        OfferLineMatchGroup group = requireGroup(projectId, groupId);
        if (request.label() != null) group.setLabel(request.label());
        if (request.notes() != null) group.setNotes(request.notes());
        return toDto(groupRepository.save(group));
    }

    @Transactional
    public void dissolve(UUID projectId, UUID groupId) {
        OfferLineMatchGroup group = requireGroup(projectId, groupId);
        lineLinkRepository.deleteByMatchGroupId(group.getId());
        groupRepository.delete(group);
    }

    private OfferLineMatchGroup requireGroup(UUID projectId, UUID groupId) {
        return groupRepository.findByIdInProject(groupId, projectId)
                .orElseThrow(NotFoundException::new);
    }

    private OfferLine requireLineInProject(UUID orgId, UUID projectId, UUID lineId) {
        OfferLine line = offerLineRepository.findById(lineId).orElseThrow(NotFoundException::new);
        Offer offer = offerRepository.findActiveByIdInOrg(line.getOfferId(), orgId)
                .orElseThrow(NotFoundException::new);
        if (!offer.getProjectId().equals(projectId)) {
            throw new MatchGroupExceptions.CrossProject();
        }
        return line;
    }

    private MatchGroupDto toDto(OfferLineMatchGroup group) {
        List<UUID> lineIds = lineLinkRepository.findByMatchGroupId(group.getId()).stream()
                .map(MatchGroupLine::getOfferLineId)
                .toList();
        return MatchGroupDto.builder()
                .id(group.getId())
                .projectId(group.getProjectId())
                .label(group.getLabel())
                .notes(group.getNotes())
                .offerLineIds(lineIds)
                .build();
    }
}

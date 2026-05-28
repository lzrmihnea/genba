package eu.px.genba.offer;

import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OfferMapper {

    OfferSummaryDto toSummary(Offer entity);

    OfferLineDto toLineDto(OfferLine entity);

    List<OfferLineDto> toLineDtos(List<OfferLine> entities);

    default OfferDto toDto(Offer entity, List<OfferLine> lines) {
        return OfferDto.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .vendorId(entity.getVendorId())
                .label(entity.getLabel())
                .receivedAt(entity.getReceivedAt())
                .validUntil(entity.getValidUntil())
                .currencyCode(entity.getCurrencyCode())
                .totalAmountExclVat(entity.getTotalAmountExclVat())
                .totalAmountInclVat(entity.getTotalAmountInclVat())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lines(toLineDtos(lines))
                .build();
    }
}

package eu.px.genba.organization;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserOrganizationMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "organization.name", target = "organizationName")
    @Mapping(source = "organization.countryCode", target = "countryCode")
    @Mapping(source = "organization.currencyCode", target = "currencyCode")
    @Mapping(source = "organization.vatRegime", target = "vatRegime")
    UserOrganizationDto toDto(UserOrganization entity);
}

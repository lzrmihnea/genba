package eu.px.genba.vendor;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorDto toDto(Vendor entity);
}

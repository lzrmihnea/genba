package eu.px.genba.attachment;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    AttachmentDto toDto(Attachment entity);
}

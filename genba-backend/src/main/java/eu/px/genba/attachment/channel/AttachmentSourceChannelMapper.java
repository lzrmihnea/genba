package eu.px.genba.attachment.channel;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttachmentSourceChannelMapper {

    AttachmentSourceChannelDto toDto(AttachmentSourceChannel entity);
}

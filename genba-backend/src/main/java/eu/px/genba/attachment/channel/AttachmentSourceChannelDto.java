package eu.px.genba.attachment.channel;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AttachmentSourceChannelDto(
        UUID id,
        String code,
        String labelEn,
        String labelRo,
        boolean systemManaged,
        int sortOrder) {
}

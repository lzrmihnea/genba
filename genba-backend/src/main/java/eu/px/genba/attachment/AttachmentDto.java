package eu.px.genba.attachment;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AttachmentDto(
        UUID id,
        String attachableType,
        UUID attachableId,
        AttachmentKind kind,
        UUID sourceChannelId,
        String fileUrl,
        String rawText,
        String originalFilename,
        String mimeType,
        Long byteSize,
        UUID uploadedBy,
        OffsetDateTime uploadedAt) {
}

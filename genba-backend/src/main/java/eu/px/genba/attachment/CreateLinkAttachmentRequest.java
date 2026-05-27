package eu.px.genba.attachment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateLinkAttachmentRequest(
        @NotBlank String attachableType,
        @NotNull UUID attachableId,
        @NotNull UUID sourceChannelId,
        @NotBlank String url) {
}

package eu.px.genba.attachment.channel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAttachmentSourceChannelRequest(
        @NotBlank
        @Size(max = 32)
        @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "code must be lowercase kebab-case")
        String code,

        @NotBlank @Size(max = 64) String labelEn,
        @NotBlank @Size(max = 64) String labelRo,
        Integer sortOrder) {
}
